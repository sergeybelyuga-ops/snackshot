import type { Firestore } from "firebase-admin/firestore";
import type { RemoteConfig } from "firebase-admin/remote-config";
import { logger } from "firebase-functions";
import { refs } from "../store/model.ts";

/** The Remote Config parameter, also read by the app (doc 11 §6.3). `"false"` turns AI off for everybody. */
export const KILL_SWITCH_PARAMETER = "ai_recognition_enabled";

/** Whether AI recognition is on. While it is off the broker creates no APIUsers. */
export interface KillSwitch {
  aiEnabled(): Promise<boolean>;
}

const CACHE_MS = 60_000;

/**
 * Reads the parameter's default value from the project's Remote Config template, cached for a
 * minute. Fails open: an unreadable template must not take AI down, and the app reads the same
 * switch itself.
 */
export class RemoteConfigKillSwitch implements KillSwitch {
  private readonly remoteConfig: RemoteConfig;
  private readonly now: () => Date;
  private cached: { value: boolean; at: number } | null = null;

  constructor(remoteConfig: RemoteConfig, now: () => Date) {
    this.remoteConfig = remoteConfig;
    this.now = now;
  }

  async aiEnabled(): Promise<boolean> {
    const t = this.now().getTime();
    if (this.cached && t - this.cached.at < CACHE_MS) return this.cached.value;
    let value = true;
    try {
      const template = await this.remoteConfig.getTemplate();
      const parameter = template.parameters[KILL_SWITCH_PARAMETER]?.defaultValue;
      value = !(parameter && "value" in parameter && parameter.value === "false");
    } catch (error) {
      logger.warn("Kill switch unreadable; AI stays on", { error: String(error) });
    }
    this.cached = { value, at: t };
    return value;
  }
}

/** Emulator and tests (Remote Config has no emulator): `settings/broker.aiRecognitionEnabled`. */
export class FirestoreKillSwitch implements KillSwitch {
  private readonly db: Firestore;

  constructor(db: Firestore) {
    this.db = db;
  }

  async aiEnabled(): Promise<boolean> {
    return (await refs(this.db).settings().get()).get("aiRecognitionEnabled") !== false;
  }
}
