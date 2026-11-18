> Source: https://ai.google.dev/gemini-api/docs/pricing

# Gemini Developer API pricing

Start building free of charge with generous limits, then scale up with prepaid then pay-as-you-go pricing for production-ready applications.

## Plans

### Free

For developers and small projects getting started with the Gemini API.

- Limited access to certain models
- Free input and output tokens
- Google AI Studio access
- Content used to improve Google's products

### Paid

For production applications that require higher volumes and advanced features.

- Higher rate limits for production deployments
- Access to Context caching
- Batch API with 50% cost reduction
- Access to Google's most advanced models
- Content not used to improve Google's products

### Enterprise

For large-scale deployments with custom needs for security, support, and compliance, powered by [Gemini Enterprise Agent Platform](https://cloud.google.com/gemini-enterprise-agent-platform).

- All Paid features, plus optional dedicated support channels
- Advanced security and compliance
- Provisioned throughput
- Volume-based discounts
- ML ops, model garden, and more

## Model pricing

Prices below are USD per 1 million tokens unless a different unit is shown. Standard is the normal paid tier. Batch, Flex, and Priority availability varies by model. Free-tier values mean free of charge; Not available means the mode is not offered.

| Model | Description | Standard input | Standard output | Context caching / other pricing |
| --- | --- | ---: | ---: | --- |
| Gemini 3.8 Flash | Most intelligent Flash model for long-horizon software engineering, autonomous agents, and complex enterprise workflows. | $0.75 through 2026-12-31; $1.50 from 2027-01-01 | $3.75 through 2026-12-31; $7.50 from 2027-01-01 | Context: $0.075 through 2026-12-31; $0.15 from 2027-01-01; storage $0.50/$1.00 per 1M tokens/hour. Google Search and Maps: 5,000 free requests/prompts per month, then $14/1,000. |
| Gemini 3.7 Flash | High-speed model for coding, agentic tool use, and multi-step execution. | $0.75 through 2026-12-31; $1.50 from 2027-01-01 | $3.75 through 2026-12-31; $7.50 from 2027-01-01 | Same caching, Search, and Maps pricing as Gemini 3.8 Flash. |
| Gemini 3.6 Flash | Previous-generation Flash model for speed and multimodal everyday tasks. | $0.75 through 2026-12-31; $1.50 from 2027-01-01 | $3.75 through 2026-12-31; $7.50 from 2027-01-01 | Same caching, Search, and Maps pricing; Search is available for testing in AI Studio on the free side. |
| Gemini 3.5 Flash | Earlier Flash model for routine, high-throughput workloads. | $1.50 | $9.00 | Context $0.15; storage $1.00 per 1M tokens/hour. Search and Maps: 5,000 free/month, then $14/1,000. |
| Gemini 3.5 Flash-Lite | Cost-efficient model for high-volume agentic tasks, translation, and simple processing. | $0.30 text/image/video/audio | $2.50 | Context $0.03; storage $1.00 per 1M tokens/hour. |
| Gemini 3.1 Flash-Lite | Cost-efficient model for high-volume processing. | $0.25 text/image/video; $0.50 audio | $1.50 | Context $0.025 text/image/video; $0.05 audio; storage $1.00 per 1M tokens/hour. |
| Gemini 2.5 Pro | Coding and complex reasoning model. | $1.25 for prompts <=200k; $2.50 for prompts >200k | $10.00 for prompts <=200k; $15.00 for prompts >200k | Context $0.125/$0.25; storage $4.50 per 1M tokens/hour. Search: 1,500 RPD free then $35/1,000; Maps: 10,000 RPD free then $25/1,000. |
| Gemini 2.5 Flash | Hybrid reasoning model with a 1M-token context window and thinking budgets. | $0.30 text/image/video; $1.00 audio | $2.50 | Context $0.03 text/image/video; $0.10 audio; storage $1.00 per 1M tokens/hour. |
| Gemini 2.5 Flash-Lite | Small, cost-effective model for scale. | $0.10 text/image/video; $0.30 audio | $0.40 | Context $0.01 text/image/video; $0.03 audio; storage $1.00 per 1M tokens/hour. |
| Gemini 3.1 Pro Preview | Multimodal, agentic, and reasoning model. | $2.00 prompts <=200k; $4.00 prompts >200k | $12.00 prompts <=200k; $18.00 prompts >200k | Context $0.20/$0.40; storage $4.50 per 1M tokens/hour. Search and Maps: 5,000 free/month then $14/1,000. |
| Gemini 3.1 Flash Live Preview | Low-latency audio-to-audio model for real-time dialogue. | $0.75 text; $3.00 audio; $1.00 image/video | $4.50 text; $12.00 audio | Audio approximately $0.005/min input and $0.018/min output. Search: 5,000 free/month then $14/1,000. |
| Gemini 3.5 Transcribe Live | Real-time speech-to-text over WebSockets. | $3.50 audio | $21.00 text | Estimated effective price approximately $0.009/min. Search not supported. |
| Gemini 3.5 Transcribe | Speech-to-text with automatic language detection, speaker diarization, timestamps, and vocabulary biasing. | $2.00 audio | $12.00 text | Estimated effective price approximately $0.005/min. Search not supported. |
| Gemini 3.1 Flash Image | Image generation model for speed and high throughput. | $0.50 text/image | $3.00 text/thinking; $60.00 images | Output images: approximately $0.045 per 0.5K, $0.067 per 1K, $0.101 per 2K, and $0.151 per 4K image. |
| Gemini 3.1 Flash Lite Image | Efficiency-focused image generation and editing model. | $0.25 text/image/video | $1.50 text/thinking; $30.00 images | Approximately $0.0336 per 1K image. |
| Gemini 3 Pro Image | Native image generation model. | $2.00 text/image | $12.00 text/thinking; $120.00 images | Approximately $0.134 per 1K/2K image and $0.24 per 4K image. |
| Gemini 2.5 Flash Image | Native image generation model. | $0.30 text/image | $0.039 per image | Output up to 1024x1024 consumes 1,290 tokens. |
| Gemini 3.1 Flash TTS Preview | Low-latency text-to-speech model. | $1.00 text | $20.00 audio | Audio tokens correspond to 25 tokens/second. |
| Gemini 2.5 Flash Preview TTS | Low-latency text-to-speech model. | $0.50 text | $10.00 audio | Audio tokens correspond to 25 tokens/second. |
| Gemini 2.5 Pro Preview TTS | More powerful low-latency text-to-speech model. | $1.00 text | $20.00 audio | Audio tokens correspond to 25 tokens/second. |
| Gemini Embedding 2 | Multimodal embeddings for text, image, video, audio, and PDFs. | $0.20 text; $0.45 image; $6.50 audio; $12.00 video | Not applicable | Image approximately $0.00012/image; audio $0.00016/second; video $0.00079/frame. |
| Gemini Embedding | Text-only embeddings. | $0.15 | Not applicable | |
| Veo 3.1 | Fast video generation with audio. | Not available | $0.40 standard 720p/1080p; $0.60 4K | Fast: $0.10 720p, $0.12 1080p, $0.30 4K. Lite: $0.05 720p, $0.08 1080p. |
| Lyria 3.5 | Music generation. | Not available | $0.08 per full song | |
| Lyria 3 | Legacy music generation models. | Not available | $0.04 per 30-second clip; $0.08 per full song | |
| Gemini Robotics ER 2 Preview | Vision-language model for robotic environments and spatial reasoning. | $2.00 | $10.00 | Context $0.20; storage $1.00 per 1M tokens/hour. |
| Gemini Robotics ER 2 Streaming Preview | Real-time robotics model using the Live API. | $2.00 | $10.00 | Supports text, image, video, audio, streaming, and function calling. |
| Gemini Robotics ER 1.6 Preview | Thinking model for physical-world interaction. | $1.00 text/image/video; $2.00 audio | $5.00 | |
| Gemini 2.5 Computer Use Preview | Browser-control agent model. | $1.25 prompts <=200k; $2.50 prompts >200k | $10.00 prompts <=200k; $15.00 prompts >200k | Charged as regular model tokens. |
| Gemma 4 | Lightweight open model built from the technology powering Gemini. | Not available | Not available | Not available. |

## Pricing for tools

Tools are priced at their own rates when used with a model. Check the [Models](https://ai.google.dev/gemini-api/docs/models) page for availability.

| Tool | Free tier | Paid tier |
| --- | --- | --- |
| Google Search | 500 RPD shared for Flash and Flash-Lite; unavailable for Pro | Gemini 2.5: 1,500 RPD free then $35/1,000 grounded prompts. Gemini 3: 5,000 free requests/month then $14/1,000. |
| Google Maps | 500 RPD; unavailable for Pro | 1,500 RPD free for Flash/Flash-Lite; 10,000 RPD free for Pro; then $25/1,000 grounded prompts. |
| Code execution | Free | Billed at standard token rates for the selected model. Runtime itself is not billed. |
| URL context | Free | Charged as input tokens. |
| Computer use | Not available | Charged as regular model tokens. |
| File search | Free | Embeddings cost $0.15/1M tokens; retrieved document tokens use model pricing. |
| Custom Tools endpoint | Not available | Same as Gemini 3.1 Pro Preview pricing. |

## Pricing for agents

Agent usage costs are calculated from underlying token consumption and tool usage.

- **Gemini Deep Research agent** — model inference is charged at standard Gemini list rates, including input, output, intermediate input, and reasoning tokens generated during agentic loops. Tool usage fees apply under the existing pricing structure.
- **Managed agents in Gemini API** — standard model rates apply. Environment compute is not billed during the preview period.
- **Antigravity Agent** — standard model rates apply. Environment compute is not billed during the preview period.

## Notes

- Agentic video understanding can use up to 88% fewer input tokens for long-form video, although token counts depend on query complexity and dynamic sampling depth.
- Document tokens for the `DOCUMENT` modality, such as PDFs, are billed at the image token rate.
- Google AI Studio usage is free of charge in available regions. See [Billing FAQs](https://ai.google.dev/gemini-api/docs/billing).
- Prices may differ from the listed prices and prices offered on Gemini Enterprise Agent Platform.
- With dynamic retrieval, only requests containing at least one grounding support URL from the web are charged for Grounding with Google Search.
- Gemini costs always apply. Rate limits are subject to change.

Last updated: 2026-09-04 UTC.
