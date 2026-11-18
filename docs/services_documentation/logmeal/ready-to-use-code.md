> Source: https://docs.logmeal.com/docs/guides-getting-started-ready-to-use-code

# Ready-to-Use Code in 35+ Languages

Obtain code samples in 35+ programming languages to start your LogMeal API integration right-away.

The LogMeal API is designed to get you productive fast. With **ready-to-use code snippets** in over **35 programming languages**, you can make your first request in minutes.

## Step 1: Import the Postman Collection

1. **Download the official `openapi.yaml` documentation file** from [this link](https://api.logmeal.com/download_yaml/).
2. Follow the [Postman instructions for importing a collection](https://learning.postman.com/docs/getting-started/importing-and-exporting/importing-data/).

## Step 2: Rename the Authorization Variable

When importing, Postman automatically assigns a placeholder variable `{{bearerToken}}` for authentication in all endpoints. To make this clearer and more useful:

- The first time you open your new Postman collection:
  1. Open the **Variables** tab of the collection.
  2. Add three new variables named `APICompanyToken`, `APIUserToken`, and `APIUserManagerToken`.
  3. For each variable, copy a valid token value. See [User Types & Access Tokens](./users-and-access-tokens.md) for details.
  4. Save the collection.

- The first time you open a specific endpoint/request from inside the Postman collection:
  1. Open the **Authorization** tab.
  2. Change the token value from `{{bearerToken}}` to your preferred variable name depending on the endpoint permissions, such as `{{APICompanyToken}}`, `{{APIUserToken}}`, or `{{APIUserManagerToken}}`.

## Step 3: Run Your First Request

1. Select the **Image-based Food Recognition > Multiple Food Dishes Segmentation** request.
2. Upload a sample food image.
3. Send the request and check the JSON response.

## Step 4: Generate Code in Your Language

Postman can instantly generate code for your preferred language or framework:

- Click **`</> Code`** in the request window.
- Choose from **Python, JavaScript, Java, Go, Swift, PHP, C#, Ruby**, and many more.
- Copy and paste the generated code into your application.

## Step 5: Download Offline Code Examples

If you prefer to run the code locally in your own environment, LogMeal provides ready-to-use scripts for **Python, JavaScript, TypeScript, PHP, Dart, Swift, Java, C#, and Kotlin**.

The package includes a sample image and instructions on how to compile and run the code for every language.

Download the code examples here: [LogMeal API code examples](https://api.logmeal.com/assets/code_samples/LogMeal_API_code_examples.zip).

## What’s Next?

- Apply the appropriate [Image Pre-processing](./image-pre-processing.md).
- Explore the full [API Reference](https://docs.logmeal.com/reference/getting-started#/).
- Implement [Food Image Recognition with Confirmation](./food-recognition-confirmation.md).
- Implement [Nutritional Information Retrieval](./nutritional-information-retrieval.md).
- Implement [Food Quantity Detection](./food-quantity-detection.md).
- Implement [Food Quantity Confirmation](./food-quantity-confirmation.md).
