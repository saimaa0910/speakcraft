package com.example.data.learningpaths

import com.example.data.ai.local.language.DynamicLocalCoachEngine
import com.example.data.ai.local.language.LocalLanguageAnalysisEngine
import com.example.data.evaluation.LabAttemptEvaluator
import com.example.data.evaluation.LabAttemptRequest
import com.example.data.evaluation.LabPracticeTarget
import com.example.data.evaluation.TranscriptUse
import com.example.speech.analysis.AcousticFeatures
import com.example.speech.android.RecordedAudioAnalyzer
import com.example.speech.pipeline.AttemptEvaluationResult
import com.example.speech.pipeline.InsightSeverity
import com.example.speech.pipeline.TranscriptAnalyzer
import com.example.speech.pipeline.TranscriptInsight
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 100% offline, evidence-grounded evaluation engine for Learning Path lessons and
 * module/capstone assessments.
 *
 * Strictly separates objective spoken/written analysis from user self-rated confidence
 * (`confidenceSelfRating`), never fabricating acoustic metrics when audio is absent.
 *
 * Speaking feedback on a recorded submission comes from the local intelligence engine
 * ([com.example.speech.pipeline.LocalIntelligencePipeline], ARC-05) — the same engine as Coach Practice and the
 * practice labs: the recording is measured on the device, the submission text is used as its transcript when it
 * can belong to the recording, and the engine's issues, tips and transcript highlights replace the text-only
 * heuristics for that part. Written (text-only) submissions keep the text analysis.
 */
object LearningPathEvaluationEngine {

    /** Engine evaluation of a submission's recording; [result] is `null` when the file could not be read. */
    private data class RecordingEvidence(
        val features: AcousticFeatures?,
        val result: AttemptEvaluationResult?,
        val transcriptUse: TranscriptUse
    ) {
        val valid: AttemptEvaluationResult.Valid? get() = result as? AttemptEvaluationResult.Valid
        val inconclusive: AttemptEvaluationResult.Inconclusive? get() = result as? AttemptEvaluationResult.Inconclusive
        /** The engine evaluated the submission text as the recording's transcript. */
        val usedTranscript: Boolean get() = valid != null && transcriptUse == TranscriptUse.USED
    }

    fun evaluateLesson(
        lesson: LearningPathLesson,
        request: LessonSubmissionRequest
    ): LearningPathFeedback = evaluateLesson(lesson, request, RecordedAudioAnalyzer::analyse)

    /** As [evaluateLesson], with the recording measurement injectable for tests. */
    internal fun evaluateLesson(
        lesson: LearningPathLesson,
        request: LessonSubmissionRequest,
        recordingAnalyzer: (File) -> AcousticFeatures?
    ): LearningPathFeedback {
        val cleanText = request.spokenOrWrittenText.trim()
        val words = LocalLanguageAnalysisEngine.tokenizeWords(cleanText)
        val wordCount = words.size
        val clampedConfidence = request.confidenceSelfRating.coerceIn(1, 5)

        val totalQuiz = lesson.quizQuestions.size
        val correctQuiz = lesson.quizQuestions.count { q ->
            request.quizAnswers[q.questionId] == q.correctIndex
        }
        val quizScorePercent = if (totalQuiz == 0) 100 else ((correctQuiz * 100.0) / totalQuiz).roundToInt()

        // Speaking evaluation by the local intelligence engine, strictly when an actual non-empty recording exists
        val recording = evaluateRecording(lesson, request, cleanText, recordingAnalyzer)
        val evidenceNote = evidenceNoteFor(recording, cleanText, request.audioFilePath)
        val engine = recording?.valid

        val hasMinimalText = wordCount >= 4
        val hasValidAudio = engine != null

        if (!hasMinimalText && !hasValidAudio) {
            val inconclusive = recording?.inconclusive
            return LearningPathFeedback(
                overallScore = if (totalQuiz > 0 && correctQuiz > 0) 20 else 5,
                passed = false,
                confidenceSelfRating = clampedConfidence,
                quizScorePercent = quizScorePercent,
                quizCorrectCount = correctQuiz,
                quizTotalCount = totalQuiz,
                whatWentWell = if (correctQuiz > 0) {
                    listOf("Answered $correctQuiz/$totalQuiz knowledge check correctly.")
                } else emptyList(),
                mistakesFound = if (inconclusive != null) {
                    listOf(
                        "Your recording could not be evaluated: ${inconclusive.reason.userMessage}",
                        "Response text is too brief ($wordCount words). Aim for at least ${lesson.targetWordCountMin} words."
                    )
                } else {
                    listOf(
                        "Response is too brief ($wordCount words) and no valid speech recording was attached. Aim for at least ${lesson.targetWordCountMin} words."
                    )
                },
                correctedExamples = listOf(
                    CorrectedExampleItem(
                        original = cleanText.ifBlank { "(empty response)" },
                        improved = lesson.exampleContrast.strongExample,
                        reason = lesson.exampleContrast.whyItWorks
                    )
                ),
                explanation = "A full response lets the local analyzer evaluate clarity, phrasing, grammar, and alignment with '${lesson.title}'.",
                coachTips = listOfNotNull(
                    inconclusive?.advice?.advice,
                    "Listen to the model example first, then record or write around ${lesson.targetWordCountMin}+ words answering: ${lesson.exercisePrompt}",
                    lesson.exampleContrast.whyItWorks
                ),
                transcriptSummary = "Add at least 4 spoken or written words to receive inline highlights.",
                transcriptInsights = emptyList(),
                rubricBreakdown = lesson.assessmentRubric.map { c ->
                    RubricCriterionScore(c.id, c.title, 0, "Insufficient response length to grade.")
                },
                recommendedNextStep = "Practice once with at least ${lesson.targetWordCountMin} words and re-submit.",
                evidenceNote = evidenceNote
            )
        }

        // Handle rare voice-only case where user recorded genuine speech audio with empty STT
        val analysisText = if (hasMinimalText) cleanText else ""
        val langResult = LocalLanguageAnalysisEngine.analyze(
            text = analysisText,
            targetWords = lesson.expectedKeyTerms
        )
        val coachResult = DynamicLocalCoachEngine.analyzeUserDraft(
            text = analysisText,
            topicHint = lesson.title
        )
        // Spoken transcript: the engine's highlights, which agree with the issues it raised (ARC-05 §8.4).
        // Written text (or a transcript that cannot belong to the recording): text highlights.
        val spokenTranscript = recording?.usedTranscript == true
        val insights: List<TranscriptInsight> = if (spokenTranscript) {
            engine!!.transcriptInsights
        } else {
            TranscriptAnalyzer.analyze(analysisText)
        }
        val recordedSeconds = recording?.features?.durationMs?.let { (it / 1000.0).roundToInt() } ?: request.audioDurationSeconds
        val transcriptSummary = when {
            analysisText.isBlank() ->
                "Audio captured (${recordedSeconds}s). Add or verify your transcript text to unlock inline word & grammar highlights."
            spokenTranscript ->
                engine!!.feedback.transcriptInsightSummary ?: TranscriptAnalyzer.composeSummary(insights)
            else -> TranscriptAnalyzer.composeSummary(insights)
        }

        // Quantitative scoring components
        val lengthRatio = (wordCount.toDouble() / lesson.targetWordCountMin.coerceAtLeast(10)).coerceIn(0.25, 1.0)
        val lengthScore = (lengthRatio * 100).roundToInt()

        val lowerText = analysisText.lowercase()
        val matchedTerms = lesson.expectedKeyTerms.filter { term ->
            Regex("""\b${Regex.escape(term.lowercase())}\w*""").containsMatchIn(lowerText)
        }
        val keyTermCoverage = if (lesson.expectedKeyTerms.isEmpty()) 80 else {
            val ratio = (matchedTerms.size.toDouble() / lesson.expectedKeyTerms.size.coerceAtLeast(1)).coerceIn(0.0, 1.0)
            (45 + ratio * 55).roundToInt()
        }

        // A spoken transcript is checked only by the engine's qualified grammar rules (SPK-02, SRS-FR-EVAL-006).
        val engineGrammar = engine?.issues.orEmpty().filter { it.code == com.example.speech.rules.IssueCode.ISSUE_GRAMMAR_POSSIBLE }
        val grammarIssueCount = if (spokenTranscript) engineGrammar.size else langResult.grammarIssues.size
        val grammarPenalty = (grammarIssueCount * 10).coerceAtMost(35)
        val fillerHedgeCount = insights.count { it.severity == InsightSeverity.WARNING }
        val fillerPenalty = (fillerHedgeCount * 6).coerceAtMost(24)

        val structuralBonus = listOf(
            coachResult.discourse.hasOpeningAnchor,
            coachResult.discourse.hasTransitionSignals,
            coachResult.discourse.hasStrongClosing || coachResult.discourse.hasConcreteExample
        ).count { it } * 7 // up to +21

        val audioOnlyCredit = analysisText.isBlank() && engine != null
        val baseDeliveryQuality = if (audioOnlyCredit) {
            // Audio-only valid recording: the engine measured speech, but there are no words to grade. This is the
            // learning path's completion credit, not a speaking measurement; the explanation says so.
            72
        } else {
            val diversityPart = (langResult.vocabularyAnalysis.vocabularyDiversityScore * 0.45).roundToInt()
            val rhythmPart = (coachResult.discourse.rhythmVarietyScore * 0.25).roundToInt()
            (46 + diversityPart + rhythmPart + structuralBonus - grammarPenalty - fillerPenalty).coerceIn(28, 100)
        }

        val rubricScores = lesson.assessmentRubric.mapIndexed { idx, criterion ->
            val rawCriterion = when (idx) {
                0 -> ((baseDeliveryQuality * 0.65) + (lengthScore * 0.35)).roundToInt()
                1 -> ((baseDeliveryQuality * 0.55) + (keyTermCoverage * 0.45)).roundToInt()
                else -> ((keyTermCoverage * 0.55) + (quizScorePercent * 0.45)).roundToInt()
            }.coerceIn(20, 100)

            val note = when {
                rawCriterion >= 85 -> "Strong execution on ${criterion.title.lowercase()}."
                rawCriterion >= 65 -> "Solid foundation; small refinements will sharpen consistency."
                else -> "Needs focused attention: review today's Before/After model contrast."
            }
            RubricCriterionScore(
                criterionId = criterion.id,
                title = criterion.title,
                scoreOutOf100 = rawCriterion,
                note = note
            )
        }

        val weightedRubricTotal = if (rubricScores.isEmpty()) {
            ((baseDeliveryQuality + lengthScore + keyTermCoverage) / 3.0).roundToInt()
        } else {
            val totalWeight = lesson.assessmentRubric.sumOf { it.maxWeightPercent }.coerceAtLeast(1)
            val sum = rubricScores.zip(lesson.assessmentRubric).sumOf { (score, spec) ->
                score.scoreOutOf100 * spec.maxWeightPercent
            }
            (sum.toDouble() / totalWeight).roundToInt()
        }

        // Combine rubric score cleanly with knowledge quiz if present
        val combinedScore = if (totalQuiz > 0) {
            ((weightedRubricTotal * 0.80) + (quizScorePercent * 0.20)).roundToInt()
        } else {
            weightedRubricTotal
        }.coerceIn(0, 100)

        // Note: confidenceSelfRating stays purely observational and NEVER alters objective combinedScore
        val checklistBoostReady = request.completedChecklistIndices.size >= 2 || totalQuiz == 0
        val passed = combinedScore >= lesson.passingScoreThreshold &&
            (hasValidAudio || wordCount >= (lesson.targetWordCountMin * 0.6).roundToInt().coerceAtLeast(10))

        val wentWell = buildStrengths(
            lesson = lesson,
            coachStrengths = coachResult.strengths,
            matchedTerms = matchedTerms,
            wordCount = wordCount,
            correctQuiz = correctQuiz,
            totalQuiz = totalQuiz,
            engine = engine
        )

        val mistakes = buildMistakes(
            lesson = lesson,
            langIssues = if (spokenTranscript) emptyList() else {
                langResult.grammarIssues.map { "${it.issueType}: '${it.originalText}' -> '${it.correctedText}'" }
            },
            engineIssues = engine?.let { LabAttemptEvaluator.issueLines(it) }.orEmpty(),
            // A spoken transcript's fillers and hedges are judged by the engine against its target bands.
            insights = if (spokenTranscript) emptyList() else insights,
            hedges = if (spokenTranscript) emptyList() else coachResult.discourse.hedgesDetected,
            vague = coachResult.discourse.vaguePhrasesDetected,
            wordCount = wordCount,
            matchedTerms = matchedTerms,
            quizCorrect = correctQuiz,
            quizTotal = totalQuiz
        )

        val corrections = buildCorrections(
            langIssues = if (spokenTranscript) emptyList() else langResult.grammarIssues,
            engineCorrections = if (spokenTranscript) engineGrammar.mapNotNull { engineCorrection(it) } else emptyList(),
            rewrites = coachResult.rewrites,
            lesson = lesson,
            cleanText = analysisText,
            matchedTerms = matchedTerms
        )

        val tips = buildCoachTips(
            lesson = lesson,
            coachSuggestions = coachResult.suggestions,
            nextMicroDrill = coachResult.nextMicroDrill,
            engine = engine,
            wordCount = wordCount,
            passed = passed,
            checklistReady = checklistBoostReady
        )

        val explanation = buildString {
            append(
                if (passed) {
                    "Day ${lesson.dayNumber} target met at $combinedScore%. "
                } else {
                    "Current score is $combinedScore% (passing target ${lesson.passingScoreThreshold}%). "
                }
            )
            if (audioOnlyCredit) {
                append(
                    "Your recording was checked on this device (speaking time and pauses); without a transcript there were no words to grade, " +
                        "so the score reflects completing the task and the knowledge check, not a measurement of your speaking. "
                )
            }
            if (engine != null) {
                append(engine.feedback.strength.message).append(' ')
                if (engine.feedback.focus.issueCode != null) {
                    append("Speaking focus: ${engine.feedback.focus.title.lowercase(Locale.ROOT)}. ")
                }
            }
            if (coachResult.narrativeSummary.isNotBlank()) {
                append(coachResult.narrativeSummary)
            } else {
                append(lesson.exampleContrast.whyItWorks)
            }
        }

        val nextStep = if (passed) {
            "Great progress on Day ${lesson.dayNumber}. Advance to the next day or launch '${lesson.linkedActivityId.substringAfter('_').replace('_', ' ')}' in Practice Studio for extra fluency."
        } else {
            "Expand your response toward ${lesson.targetWordCountMin}+ focused words, incorporate key terms (${lesson.expectedKeyTerms.take(3).joinToString(", ")}), and submit again."
        }

        return LearningPathFeedback(
            overallScore = combinedScore,
            passed = passed,
            confidenceSelfRating = clampedConfidence,
            quizScorePercent = quizScorePercent,
            quizCorrectCount = correctQuiz,
            quizTotalCount = totalQuiz,
            whatWentWell = wentWell,
            mistakesFound = mistakes,
            correctedExamples = corrections,
            explanation = explanation,
            coachTips = tips,
            transcriptSummary = transcriptSummary,
            transcriptInsights = insights,
            rubricBreakdown = rubricScores,
            recommendedNextStep = nextStep,
            evidenceNote = evidenceNote
        )
    }

    fun evaluateModuleAssessment(
        spec: ModuleAssessmentSpec,
        submission: ModuleAssessmentSubmission
    ): ModuleAssessmentEvaluation {
        val totalQ = spec.scenarioQuestions.size.coerceAtLeast(1)
        val correctQ = spec.scenarioQuestions.count { q ->
            submission.scenarioAnswers[q.questionId] == q.correctIndex
        }
        val quizPercent = ((correctQ * 100.0) / totalQ).roundToInt()

        // Synthesize a temporary virtual lesson spec to reuse exact objective analysis pipeline cleanly
        val syntheticLesson = LearningPathLesson(
            lessonId = spec.assessmentId,
            programId = spec.programId,
            moduleId = spec.moduleId,
            dayNumber = 0,
            title = spec.title,
            subtitle = spec.overview,
            learningObjective = spec.overview,
            coreConceptExplanation = spec.overview,
            keyTakeaways = spec.expectedElements,
            exampleContrast = BeforeAfterContrast(
                weakExample = "Incomplete or unfocused assessment submission.",
                strongExample = "Direct, structured response addressing: ${spec.expectedElements.joinToString(", ")}.",
                whyItWorks = "Module assessments measure transfer across real scenarios and cohesive delivery."
            ),
            focusKeywords = spec.expectedElements,
            exerciseType = LessonExerciseType.SCENARIO_ROLEPLAY,
            exercisePrompt = spec.practicalTaskPrompt,
            practiceChecklist = spec.expectedElements,
            ttsModelScript = "",
            targetWordCountMin = spec.minWords,
            estimatedMinutes = spec.suggestedMinutes,
            passingScoreThreshold = spec.passingScoreThreshold,
            linkedActivityId = "m04_structured_answers",
            quizQuestions = emptyList(),
            assessmentRubric = spec.rubric,
            expectedKeyTerms = spec.expectedElements.flatMap { it.split(" ") }.filter { it.length >= 4 }.distinct().take(6)
        )

        val practicalFeedback = evaluateLesson(
            lesson = syntheticLesson,
            request = LessonSubmissionRequest(
                programId = spec.programId,
                dayNumber = 0,
                spokenOrWrittenText = submission.practicalResponseText,
                audioFilePath = submission.audioFilePath,
                audioDurationSeconds = submission.audioDurationSeconds,
                confidenceSelfRating = submission.confidenceSelfRating,
                practiceDurationSeconds = submission.practiceDurationSeconds
            )
        )

        val practicalPercent = practicalFeedback.overallScore
        val combined = ((quizPercent * 0.40) + (practicalPercent * 0.60)).roundToInt().coerceIn(0, 100)
        val passed = combined >= spec.passingScoreThreshold && quizPercent >= 50 && practicalPercent >= 50

        val mergedTips = buildList {
            if (quizPercent < 70) {
                add("Review the missed scenario explanations carefully—practical judgment accounts for 40% of this checkpoint.")
            }
            addAll(practicalFeedback.coachTips)
        }.distinct().take(4)

        val enrichedFeedback = practicalFeedback.copy(
            overallScore = combined,
            passed = passed,
            quizScorePercent = quizPercent,
            quizCorrectCount = correctQ,
            quizTotalCount = spec.scenarioQuestions.size,
            explanation = if (passed) {
                "Checkpoint passed at $combined% (Scenario Quiz $quizPercent%, Practical Task $practicalPercent%). ${practicalFeedback.explanation}"
            } else {
                "Scored $combined% (target ${spec.passingScoreThreshold}%). Scenario Quiz: $quizPercent%, Practical Task: $practicalPercent%. Strengthen the weaker half and retake cleanly."
            },
            coachTips = mergedTips,
            recommendedNextStep = if (passed) {
                if (spec.isCapstone) "Capstone completed! Explore your recommended follow-on Learning Path."
                else "Module passed cleanly—proceed to the next module's lessons."
            } else {
                "Refine your practical draft around (${spec.expectedElements.joinToString(", ")}) or re-check missed scenario questions and retake."
            }
        )

        return ModuleAssessmentEvaluation(
            assessmentId = spec.assessmentId,
            moduleId = spec.moduleId,
            programId = spec.programId,
            quizScorePercent = quizPercent,
            practicalScorePercent = practicalPercent,
            combinedScore = combined,
            passed = passed,
            feedback = enrichedFeedback,
            evaluatedAtEpochMs = System.currentTimeMillis()
        )
    }

    /**
     * Measures the submission's recording and evaluates it with the local intelligence engine; `null` when there
     * is no non-empty recording. Lessons are free speaking (`TPL-ANSWER` with the lesson's word target), or a
     * structure check when the lesson names PREP or STAR.
     */
    private fun evaluateRecording(
        lesson: LearningPathLesson,
        request: LessonSubmissionRequest,
        cleanText: String,
        recordingAnalyzer: (File) -> AcousticFeatures?
    ): RecordingEvidence? {
        val file = request.audioFilePath?.takeIf { it.isNotBlank() }?.let { File(it) }
        if (file == null || !file.exists() || file.length() <= 0L) return null
        val features = runCatching { recordingAnalyzer(file) }.getOrNull()
            ?: return RecordingEvidence(features = null, result = null, transcriptUse = TranscriptUse.NONE)
        val fromPrompt = LabPracticeTarget.fromPrompt(lesson.title, lesson.exercisePrompt)
        val target = if (fromPrompt.templateId == LabPracticeTarget.TPL_STRUCT) fromPrompt
        else LabPracticeTarget.answer(targetMinWords = lesson.targetWordCountMin.takeIf { it > 0 })
        val attempt = LabAttemptEvaluator.runEngine(
            request = LabAttemptRequest(
                sessionId = "${lesson.lessonId}_${file.name}",
                moduleType = "LESSON_PRACTICE",
                promptTitle = lesson.title,
                target = target,
                localMediaPath = file.absolutePath
            ),
            features = features,
            transcript = cleanText
        )
        return RecordingEvidence(features, attempt.result, attempt.transcriptUse)
    }

    private fun evidenceNoteFor(recording: RecordingEvidence?, cleanText: String, audioFilePath: String?): String {
        if (recording == null) {
            return if (cleanText.isNotBlank()) {
                "Evaluated from written/transcribed response (no audio file attached)."
            } else {
                "No audio recording or transcript detected yet."
            }
        }
        val features = recording.features
            ?: return "Audio file attached (${audioFilePath?.let { File(it).length() / 1024 } ?: 0} KB) but it could not be read on this device; evaluated from the text only."
        val measured = String.format(
            Locale.ROOT, "%.1f s recorded, %.1f s speaking", features.durationMs / 1000.0, features.voicedDurationMs / 1000.0
        )
        return when (val result = recording.result) {
            is AttemptEvaluationResult.Inconclusive ->
                "Recording not evaluated ($measured): ${result.reason.userMessage}"
            is AttemptEvaluationResult.Valid -> {
                val words = when (recording.transcriptUse) {
                    TranscriptUse.USED -> "audio + your transcript"
                    TranscriptUse.NONE -> "audio only (no transcript)"
                    TranscriptUse.INCOMPLETE -> "audio only (the text did not match the recording's length, so it was graded as written text)"
                }
                "Evaluated on this device by the speech engine ${result.evaluationVersion} ($measured; $words)."
            }
            null -> "Audio file attached; evaluated from the text only."
        }
    }

    /** An engine grammar observation as a before/after correction. */
    private fun engineCorrection(issue: com.example.speech.rules.DetectedIssue): CorrectedExampleItem? {
        val (before, after, why) = LabAttemptEvaluator.grammarRewrite(issue) ?: return null
        return CorrectedExampleItem(original = before, improved = after, reason = why.ifBlank { issue.code.title })
    }

    private fun buildStrengths(
        lesson: LearningPathLesson,
        coachStrengths: List<String>,
        matchedTerms: List<String>,
        wordCount: Int,
        correctQuiz: Int,
        totalQuiz: Int,
        engine: AttemptEvaluationResult.Valid?
    ): List<String> {
        val out = mutableListOf<String>()
        // Speaking strength chosen by the engine from measured metrics (never praise for an unmeasured value).
        engine?.let { out.add(it.feedback.strength.message) }
        if (totalQuiz > 0 && correctQuiz == totalQuiz) {
            out.add("Knowledge check mastery: answered $correctQuiz/$totalQuiz correctly.")
        }
        if (wordCount >= lesson.targetWordCountMin) {
            out.add("Substantial response depth ($wordCount words, meeting the ${lesson.targetWordCountMin}-word target).")
        }
        if (matchedTerms.isNotEmpty()) {
            out.add("Applied lesson focus vocabulary accurately: ${matchedTerms.joinToString(", ") { "'$it'" }}.")
        }
        out.addAll(coachStrengths.take(2))
        if (out.isEmpty()) {
            out.add("Engaged meaningfully with today's prompt on '${lesson.title}'.")
        }
        return out.distinct().take(4)
    }

    private fun buildMistakes(
        lesson: LearningPathLesson,
        langIssues: List<String>,
        engineIssues: List<String>,
        insights: List<TranscriptInsight>,
        hedges: List<String>,
        vague: List<String>,
        wordCount: Int,
        matchedTerms: List<String>,
        quizCorrect: Int,
        quizTotal: Int
    ): List<String> {
        val out = mutableListOf<String>()
        // Speaking mistakes found by the engine in the recording (and its transcript) come first.
        out.addAll(engineIssues.take(3))
        out.addAll(langIssues.take(3))
        val fillers = insights.filter { it.issueTitle.contains("Filler", ignoreCase = true) }
            .map { it.highlightedText.lowercase() }.distinct()
        if (fillers.isNotEmpty()) {
            out.add("Hesitation fillers detected (${fillers.joinToString(", ") { "'$it'" }})—replace with a brief silent beat.")
        }
        if (hedges.isNotEmpty()) {
            out.add("Softening hedge language spotted (${hedges.take(3).joinToString(", ") { "'$it'" }})—state recommendations directly.")
        }
        if (vague.isNotEmpty()) {
            out.add("Vague placeholder words spotted (${vague.take(3).joinToString(", ") { "'$it'" }})—swap for concrete nouns/verbs.")
        }
        if (wordCount in 1 until lesson.targetWordCountMin) {
            out.add("Response length ($wordCount words) is below the suggested ${lesson.targetWordCountMin}-word target for full practice.")
        }
        if (lesson.expectedKeyTerms.isNotEmpty() && matchedTerms.isEmpty() && wordCount > 0) {
            out.add("Missed today's target concepts (${lesson.expectedKeyTerms.take(3).joinToString(", ")})—weave them in deliberately.")
        }
        if (quizTotal > 0 && quizCorrect < quizTotal) {
            out.add("Missed ${quizTotal - quizCorrect} knowledge check item(s)—review the explanation card.")
        }
        return out.distinct().take(5)
    }

    private fun buildCorrections(
        langIssues: List<com.example.data.ai.local.language.GrammarIssue>,
        engineCorrections: List<CorrectedExampleItem>,
        rewrites: List<DynamicLocalCoachEngine.SentenceRewrite>,
        lesson: LearningPathLesson,
        cleanText: String,
        matchedTerms: List<String>
    ): List<CorrectedExampleItem> {
        val out = mutableListOf<CorrectedExampleItem>()
        out.addAll(engineCorrections.take(2))
        langIssues.take(2).forEach { issue ->
            out.add(
                CorrectedExampleItem(
                    original = issue.originalText,
                    improved = issue.correctedText,
                    reason = issue.explanation
                )
            )
        }
        rewrites.take(2).forEach { rw ->
            out.add(
                CorrectedExampleItem(
                    original = rw.original,
                    improved = rw.improved,
                    reason = rw.why
                )
            )
        }
        if (out.isEmpty() && (cleanText.length < 60 || matchedTerms.isEmpty())) {
            out.add(
                CorrectedExampleItem(
                    original = cleanText.ifBlank { lesson.exampleContrast.weakExample },
                    improved = lesson.exampleContrast.strongExample,
                    reason = lesson.exampleContrast.whyItWorks
                )
            )
        }
        return out.distinctBy { it.original to it.improved }.take(3)
    }

    private fun buildCoachTips(
        lesson: LearningPathLesson,
        coachSuggestions: List<String>,
        nextMicroDrill: String,
        engine: AttemptEvaluationResult.Valid?,
        wordCount: Int,
        passed: Boolean,
        checklistReady: Boolean
    ): List<String> {
        val out = mutableListOf<String>()
        // The engine's focus advice and coach tips for the recording (pace, pauses, fillers … against its bands).
        engine?.let { out.addAll(LabAttemptEvaluator.tipLines(it).take(2)) }
        out.addAll(coachSuggestions.take(2))
        if (nextMicroDrill.isNotBlank()) out.add(nextMicroDrill)
        if (!checklistReady) {
            out.add("Tick off the self-practice checklist steps before recording for even sharper focus.")
        }
        if (passed && wordCount >= lesson.targetWordCountMin) {
            out.add("Model comparison: listen to '${lesson.title}' TTS cadence once more and note how cleanly endings drop.")
        }
        return out.distinct().take(4)
    }
}
