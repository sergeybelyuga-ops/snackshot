> Source: https://docs.logmeal.com/docs/guides-essential-concepts-plans-limits

# Plans & Limits

The LogMeal API offers subscription plans. Each plan unlocks different feature sets, subject to quotas and request limits. This guide explains how plans work, which limits apply, and how to check usage programmatically.

## Subscription Plans

### Plan Access at a Glance

| Plan | What it unlocks | Example features |
| --- | --- | --- |
| Free (Trial) | Full access to all features | Unlimited access to all endpoints during the trial |
| Analyse | Core food recognition and nutrition extraction | Dish recognition, ingredients information, nutritional information |
| Monitor | Tracking and management tools | Intake history, daily goals, custom occasions, APIUserManager endpoints |
| Recommend | AI-driven scoring and recommendations | Variety score, Nutri-Scores, recipe recommendations, food quantity detection |
| Custom | Enterprise or tailored services | Dysphagia feedback, LogMeal Kiosk integration |

When you first sign up, you get a 30-day Free trial with unlimited access to all endpoints across all plans. After 30 days, choose Analyse, Monitor, Recommend, or Custom to continue.

All higher-tier plans include the features of lower-tier plans. For up-to-date pricing, see the [LogMeal Pricing Page](https://logmeal.com/api/pricing/).

## Request Limits and Quotas

All plans are subject to quotas and request limitations. Contact LogMeal if you want to upgrade plan limitations.

- **Monthly credits** are shared across all APIUsers. Calls beyond the monthly limit consume extra credits.
- **Daily request limits** limit the number of requests each user can send per day. Standard plans allow 20 image recognitions per day per APIUser.
- **Per-second request limits** restrict real-time request throughput. Standard plans allow 1 image per second per APIUser.
- **Other per-endpoint limits** may apply and are documented with the endpoint.

If limits are exceeded, the API returns HTTP 429 Too Many Requests.

Even on the Free Trial tier, requests are subject to the same daily and per-second limits as standard plans. No extra monthly usage is allowed during the trial.

## How to Check Your Limits

### Usage and Limits via Response Headers

For credit-consuming endpoints, LogMeal returns usage headers in the HTTP response.

#### End-User (APIUser token) — Daily Requests (Rolling 24h)

- `RateLimit-Limit`: Maximum requests allowed for the APIUser token within a rolling 24-hour window.
- `RateLimit-Remaining`: Requests still available for the APIUser token after the call within the rolling 24-hour window.
- `RateLimit-Reset`: Number of seconds until the next request slot becomes available. If `RateLimit-Remaining` is `0`, retry after `RateLimit-Reset` seconds.

#### Company (APICompany billing period) — Credits

- `X-Account-Credits-Used`: Total credits consumed by the company since the start of the current billing period.
- `X-Account-Credits-Remaining`: Credits remaining for the company until the next billing date.

Example:

```bash
curl -i -X POST "https://api.logmeal.com/..." \
  -H "Authorization: Bearer <token>" \
  -F "image=@./food.jpg"
```

You can also check quotas and current usage with `GET /v2/info/limitations`, which does not spend credits.

## Combining Plans and User Types

Plan access works in combination with user type tokens:

- **APICompany** — company-wide administrator that creates and manages users.
- **APIUser** — end-user of the app, one per person.
- **APIUserManager** — manager or clinician with access to multiple APIUsers.

An endpoint may require both a minimum plan level and a specific user type. For example, Custom Occasions requires Monitor or higher and APICompany/APIUserManager token access.

## Next Steps

- After the 30-day Free Trial ends, choose a subscription plan on the [LogMeal Pricing Page](https://logmeal.com/api/pricing/).
- Learn about [User Types & Access Tokens](./guides-essential-concepts-users-and-access-tokens).
- Explore [Food Recognition Capabilities](./guides-essential-concepts-recognition-capabilities).
