> Source: https://ai.google.dev/gemini-api/docs

# Gemini API

The Gemini API is the fastest path from prompt to production with Gemini, Veo, Nano Banana, and more. It lets you integrate these generative models into your applications to generate text and images, analyze multimodal inputs, and build conversational agents.

```python
from google import genai

client = genai.Client()

interaction = client.interactions.create(
    model="gemini-3.8-flash",
    input="Explain how AI works in a few words"
)

print(interaction.output_text)
```

- [Get Started](https://ai.google.dev/gemini-api/docs/get-started)
- [Create API Key](https://aistudio.google.com/apikey)

## Meet the models

- [Gemini 3.8 Flash](https://ai.google.dev/gemini-api/docs/models/gemini-3.8-flash) — our most intelligent Flash model, engineered for long-horizon software engineering, autonomous agents, and complex enterprise workflows.
- [Gemini 3.5 Flash-Lite](https://ai.google.dev/gemini-api/docs/models/gemini-3.5-flash-lite) — a high-volume, cost-sensitive model optimized for low-latency, high-throughput subagent tasks.
- [Gemini 3.1 Pro](https://ai.google.dev/gemini-api/docs/models/gemini-3.1-pro-preview) — a multimodal model for advanced understanding and reasoning.
- [Nano Banana 2 and Nano Banana Pro](https://ai.google.dev/gemini-api/docs/image-generation) — image generation and editing models.
- [Gemini Omni Flash](https://ai.google.dev/gemini-api/docs/omni) — a video generation and editing model.
- [Gemini 3.5 Transcribe](https://ai.google.dev/gemini-api/docs/models/gemini-3.5-transcribe) — a low-latency speech-to-text model with language detection, speaker diarization, and word timestamps.
- [Gemini Robotics](https://ai.google.dev/gemini-api/docs/robotics-overview) — vision-language models for robotics and physical-world reasoning.
- [View all models](https://ai.google.dev/gemini-api/docs/models)

## Explore capabilities

- [Image Generation](https://ai.google.dev/gemini-api/docs/image-generation) — generate and edit contextual images with Nano Banana.
- [Long Context](https://ai.google.dev/gemini-api/docs/long-context) — input millions of tokens and derive understanding from unstructured images, videos, and documents.
- [Structured Outputs](https://ai.google.dev/gemini-api/docs/structured-output) — constrain Gemini to respond with JSON suitable for automated processing.
- [Function Calling](https://ai.google.dev/gemini-api/docs/function-calling) — build agentic workflows by connecting Gemini to external APIs and tools.
- [Video Generation with Veo 3.1](https://ai.google.dev/gemini-api/docs/video) — create high-quality video from text or image prompts.
- [Voice Agents with Live API](https://ai.google.dev/gemini-api/docs/live) — build real-time voice applications and agents.
- [Tools](https://ai.google.dev/gemini-api/docs/tools) — connect Gemini to Google Search, URL Context, Google Maps, Code Execution, and Computer Use.
- [Document Understanding](https://ai.google.dev/gemini-api/docs/document-processing) — process up to 1000 pages of PDF files with multimodal understanding or other text-based file types.
- [Thinking](https://ai.google.dev/gemini-api/docs/thinking) — explore thinking capabilities for complex tasks and agents.

## Interactions API

The Interactions API has become the default interface as of June 2026 and is the recommended way to build with Gemini models and agents. When starting a new project, use the Interactions API. The `generateContent` API remains supported but is now considered legacy.

- [Interactions Overview](https://ai.google.dev/gemini-api/docs/interactions-overview) — conversation state, messages, and output formats.
- [Migration Guide](https://ai.google.dev/gemini-api/docs/migrate-to-interactions)
- [Streaming](https://ai.google.dev/gemini-api/docs/streaming)

## Related resources

- [API Reference](https://ai.google.dev/api)
- [Google AI Studio](https://aistudio.google.com/)
- [Developer Community](https://discuss.ai.google.dev/c/gemini-api/4)
- [Status](https://aistudio.google.com/status)

Last updated: 2026-09-04 UTC.
