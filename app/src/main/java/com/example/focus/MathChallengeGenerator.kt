package com.example.focus

import kotlin.random.Random

/**
 * Procedural 10th-standard mathematics challenge generator for Focus Mode unlock.
 * Ensures questions are authentic, non-trivial, distinct, and cleanly verifiable.
 */
data class MathQuestion(
  val id: Int,
  val topic: String,
  val questionHi: String,
  val questionEn: String,
  val hint: String,
  val correctAnswer: Int
)

object MathChallengeGenerator {

  /**
   * Generates 3 distinct, fresh 10th-class level mathematics questions.
   */
  fun generateThreeQuestions(): List<MathQuestion> {
    val generators = listOf<() -> MathQuestion>(
      { generateQuadraticRootQuestion() },
      { generateArithmeticProgressionQuestion() },
      { generateLinearSystemQuestion() },
      { generateTrigonometryQuestion() },
      { generateCoordinateDistanceQuestion() },
      { generateMensurationCircleQuestion() },
      { generateStatisticsMeanQuestion() }
    ).shuffled()

    return generators.take(3).mapIndexed { index, gen ->
      val q = gen()
      q.copy(id = index + 1)
    }
  }

  /**
   * 1. Quadratic Equation: Positive Root
   * (x - r1)(x - r2) = 0 => x^2 - (r1+r2)x + (r1*r2) = 0
   */
  private fun generateQuadraticRootQuestion(): MathQuestion {
    val r1 = Random.nextInt(2, 6)
    val r2 = Random.nextInt(6, 12) // larger root
    val b = r1 + r2
    val c = r1 * r2

    return MathQuestion(
      id = 1,
      topic = "द्विघात समीकरण (Quadratic Equation)",
      questionHi = "समीकरण x² - ${b}x + $c = 0 का सबसे बड़ा धनात्मक मूल (Larger Root) क्या है?",
      questionEn = "Find the larger root of the quadratic equation x² - ${b}x + $c = 0.",
      hint = "गुणनखंड करें: (x - $r1)(x - $r2) = 0",
      correctAnswer = r2
    )
  }

  /**
   * 2. Arithmetic Progression (AP): Find n-th term
   * a_n = a + (n - 1) * d
   */
  private fun generateArithmeticProgressionQuestion(): MathQuestion {
    val a = Random.nextInt(3, 9)
    val d = Random.nextInt(3, 7)
    val n = Random.nextInt(8, 14)
    val ans = a + (n - 1) * d

    val t1 = a
    val t2 = a + d
    val t3 = a + 2 * d

    return MathQuestion(
      id = 2,
      topic = "समान्तर श्रेढ़ी (Arithmetic Progression)",
      questionHi = "समान्तर श्रेढ़ी $t1, $t2, $t3 ... का ${n}वाँ पद (aₙ) क्या होगा?",
      questionEn = "Find the ${n}th term of the AP: $t1, $t2, $t3 ...",
      hint = "सूत्र: aₙ = a + (n - 1)d",
      correctAnswer = ans
    )
  }

  /**
   * 3. System of 2 Linear Equations:
   * x + y = S, x - y = D => x = (S+D)/2
   */
  private fun generateLinearSystemQuestion(): MathQuestion {
    val x = Random.nextInt(5, 12)
    val y = Random.nextInt(2, 6)
    val sum = x + y
    val diff = x - y

    return MathQuestion(
      id = 3,
      topic = "दो चरों वाले रैखिक समीकरण (Linear Equations)",
      questionHi = "यदि x + y = $sum और x - y = $diff हो, तो x का मान क्या होगा?",
      questionEn = "If x + y = $sum and x - y = $diff, what is the value of x?",
      hint = "दोनों समीकरणों को जोड़ें: 2x = $sum + $diff",
      correctAnswer = x
    )
  }

  /**
   * 4. Trigonometry standard values:
   * A * sin(30°) + B * tan(45°) or similar
   */
  private fun generateTrigonometryQuestion(): MathQuestion {
    val a = Random.nextInt(2, 6) * 2 // even number so a * 1/2 is integer
    val b = Random.nextInt(2, 7)
    // a * sin(30°) + b * tan(45°) = a*(1/2) + b*(1) = a/2 + b
    val ans = (a / 2) + b

    return MathQuestion(
      id = 4,
      topic = "त्रिकोणमिति (Trigonometry)",
      questionHi = "$a·sin(30°) + $b·tan(45°) का मान क्या है?",
      questionEn = "Evaluate: $a·sin(30°) + $b·tan(45°).",
      hint = "sin(30°) = 1/2, tan(45°) = 1",
      correctAnswer = ans
    )
  }

  /**
   * 5. Coordinate Geometry: Distance formula with Pythagorean Triples
   * (3, 4, 5) or (6, 8, 10) or (5, 12, 13)
   */
  private fun generateCoordinateDistanceQuestion(): MathQuestion {
    val triples = listOf(
      Triple(3, 4, 5),
      Triple(6, 8, 10),
      Triple(5, 12, 13)
    )
    val selected = triples.random()
    val dx = selected.first
    val dy = selected.second
    val dist = selected.third

    val x1 = Random.nextInt(1, 5)
    val y1 = Random.nextInt(1, 5)
    val x2 = x1 + dx
    val y2 = y1 + dy

    return MathQuestion(
      id = 5,
      topic = "निर्देशांक ज्यामिति (Coordinate Geometry)",
      questionHi = "बिंदु ($x1, $y1) और ($x2, $y2) के बीच की दूरी (Distance) क्या है?",
      questionEn = "Find the distance between the points ($x1, $y1) and ($x2, $y2).",
      hint = "दूरी = √[(x₂ - x₁)² + (y₂ - y₁)²]",
      correctAnswer = dist
    )
  }

  /**
   * 6. Mensuration: Circumference of circle
   * r = 7 * k, Circumference = 2 * (22/7) * 7k = 44 * k
   */
  private fun generateMensurationCircleQuestion(): MathQuestion {
    val k = Random.nextInt(1, 4)
    val r = 7 * k
    val ans = 44 * k

    return MathQuestion(
      id = 6,
      topic = "क्षेत्रमिति (Mensuration)",
      questionHi = "यदि एक वृत्त की त्रिज्या r = $r सेमी हो (π = 22/7), तो वृत्त की परिधि (Circumference) कितने सेमी होगी?",
      questionEn = "If radius of a circle r = $r cm (using π = 22/7), find its circumference in cm.",
      hint = "परिधि = 2πr = 2 × (22/7) × $r",
      correctAnswer = ans
    )
  }

  /**
   * 7. Statistics: Mean of numbers
   */
  private fun generateStatisticsMeanQuestion(): MathQuestion {
    val targetMean = Random.nextInt(10, 25)
    val totalSum = targetMean * 5
    val n1 = targetMean - 4
    val n2 = targetMean - 2
    val n3 = targetMean + 1
    val n4 = targetMean + 2
    val n5 = totalSum - (n1 + n2 + n3 + n4)

    val numbers = listOf(n1, n2, n3, n4, n5).shuffled()

    return MathQuestion(
      id = 7,
      topic = "सांख्यिकी (Statistics - Mean)",
      questionHi = "संख्याओं ${numbers.joinToString(", ")} का समांतर माध्य (Mean/Average) क्या है?",
      questionEn = "Find the arithmetic mean (average) of the numbers: ${numbers.joinToString(", ")}.",
      hint = "माध्य = (सभी संख्याओं का योग) / 5",
      correctAnswer = targetMean
    )
  }

  /**
   * Verifies the answer against user input.
   */
  fun isAnswerCorrect(question: MathQuestion, rawInput: String): Boolean {
    val parsed = rawInput.trim().toIntOrNull() ?: return false
    return parsed == question.correctAnswer
  }
}
