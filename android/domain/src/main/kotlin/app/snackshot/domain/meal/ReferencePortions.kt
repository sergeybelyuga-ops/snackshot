package app.snackshot.domain.meal

import java.util.Locale

/**
 * Typical weights of single foods, for when LogMeal's portion is missing or uncertain (F-01 step 6,
 * F-02). Values are the edible part of one medium item, or one usual serving of a drink at
 * 1 g per ml, rounded from USDA FoodData Central household measures. They are only a starting
 * point: the review always shows the grams for the user to correct.
 *
 * Only foods eaten as one item or one glass are listed. Foods served in any amount (rice, salad,
 * pasta) have no useful average and fall through to LogMeal's grams.
 */
object ReferencePortions {
    private val GRAMS: Map<String, Double> = mapOf(
        // Fruit
        "apple" to 182.0,
        "banana" to 118.0,
        "orange" to 131.0,
        "pear" to 178.0,
        "peach" to 150.0,
        "nectarine" to 142.0,
        "plum" to 66.0,
        "kiwi" to 69.0,
        "kiwifruit" to 69.0,
        "tangerine" to 88.0,
        "mandarin" to 88.0,
        "mandarin orange" to 88.0,
        "avocado" to 150.0,
        // Vegetables
        "tomato" to 123.0,
        "cucumber" to 301.0,
        "carrot" to 61.0,
        "potato" to 173.0,
        "bell pepper" to 119.0,
        // Eggs
        "egg" to 50.0,
        "boiled egg" to 50.0,
        "hard boiled egg" to 50.0,
        "fried egg" to 46.0,
        // Bakery
        "croissant" to 57.0,
        "bagel" to 105.0,
        "muffin" to 113.0,
        "pizza slice" to 107.0,
        // Drinks
        "coffee" to 240.0,
        "black coffee" to 240.0,
        "tea" to 240.0,
        "milk" to 244.0,
        "orange juice" to 248.0,
        "beer" to 356.0,
        "wine" to 147.0,
        "red wine" to 147.0,
        "white wine" to 147.0,
    )

    /** The typical grams for [foodName], or null when it is not a listed single food. */
    fun gramsFor(foodName: String): Double? {
        val name = foodName.trim().lowercase(Locale.ROOT).replace('-', ' ').replace(Regex("\\s+"), " ")
        // Exact names only: "apples" is several apples, so one apple's weight would be wrong.
        return GRAMS[name]
    }
}
