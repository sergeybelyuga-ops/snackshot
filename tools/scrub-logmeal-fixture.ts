// Scrubs a recorded LogMeal response for use as a shared test fixture
// (docs/Architecture/12_dev-and-test-environment.md §3.1).
//
//   node tools/scrub-logmeal-fixture.ts <in.json> <out.json> [--image-id 1000001]
//
// Rules (keep in sync with test-fixtures/logmeal/README.md):
// - `imageId` (account-specific) → the fixed fake value given by --image-id.
// - Any key named token / api_user_id / user_id / userId / username → removed.
// - Dish, food-family and food-type ids are LogMeal catalogue ids and are kept.
// - UTF-8 BOM stripped, output pretty-printed with a trailing newline.
import { readFileSync, writeFileSync } from "node:fs";

const [input, output] = process.argv.slice(2);
if (!input || !output) {
  console.error("usage: node tools/scrub-logmeal-fixture.ts <in.json> <out.json> [--image-id N]");
  process.exit(2);
}
const i = process.argv.indexOf("--image-id");
const fakeImageId = i >= 0 ? Number(process.argv[i + 1]) : 1000001;

const DROP = new Set(["token", "api_user_id", "user_id", "userId", "username"]);

function scrub(v: unknown): unknown {
  if (Array.isArray(v)) return v.map(scrub);
  if (v && typeof v === "object") {
    const out: Record<string, unknown> = {};
    for (const [k, val] of Object.entries(v)) {
      if (DROP.has(k)) continue;
      out[k] = k === "imageId" ? fakeImageId : scrub(val);
    }
    return out;
  }
  return v;
}

const text = readFileSync(input, "utf8").replace(/^﻿/, "");
writeFileSync(output, JSON.stringify(scrub(JSON.parse(text)), null, 2) + "\n");
console.log(`${input} → ${output} (imageId=${fakeImageId})`);
