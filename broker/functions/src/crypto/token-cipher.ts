import { KeyManagementServiceClient } from "@google-cloud/kms";
import { createCipheriv, createDecipheriv, createHash, randomBytes } from "node:crypto";

/**
 * Encrypts APIUser tokens before they reach Firestore (doc 11 §4). The client id is bound as
 * associated data, so a ciphertext copied to another client record does not decrypt.
 */
export interface TokenCipher {
  encrypt(token: string, clientId: string): Promise<string>;
  decrypt(ciphertext: string, clientId: string): Promise<string>;
}

const aad = (clientId: string) => Buffer.from(`snackshot:apiuser-token:${clientId}`);

/** Cloud KMS, key `broker/apiuser-token` in europe-west3; only `broker@` may use it. */
export class KmsTokenCipher implements TokenCipher {
  private readonly keyName: string;
  private readonly kms: KeyManagementServiceClient;

  constructor(keyName: string, kms: KeyManagementServiceClient = new KeyManagementServiceClient()) {
    this.keyName = keyName;
    this.kms = kms;
  }

  async encrypt(token: string, clientId: string) {
    const [result] = await this.kms.encrypt({ name: this.keyName, plaintext: Buffer.from(token), additionalAuthenticatedData: aad(clientId) });
    if (!result.ciphertext) throw new Error("KMS returned no ciphertext");
    return `kms1:${Buffer.from(result.ciphertext as Uint8Array).toString("base64")}`;
  }

  async decrypt(ciphertext: string, clientId: string) {
    if (!ciphertext.startsWith("kms1:")) throw new Error("Not a KMS token ciphertext");
    const [result] = await this.kms.decrypt({
      name: this.keyName,
      ciphertext: Buffer.from(ciphertext.slice(5), "base64"),
      additionalAuthenticatedData: aad(clientId),
    });
    if (!result.plaintext) throw new Error("KMS returned no plaintext");
    return Buffer.from(result.plaintext as Uint8Array).toString("utf8");
  }
}

/**
 * Emulator only (`fake` mode): AES-256-GCM with a key derived from a constant. It protects
 * nothing and exists so the emulator stores the same shape of data as the cloud.
 */
export class EmulatorTokenCipher implements TokenCipher {
  private readonly key = createHash("sha256").update("snackshot-emulator-only").digest();

  async encrypt(token: string, clientId: string) {
    const iv = randomBytes(12);
    const cipher = createCipheriv("aes-256-gcm", this.key, iv).setAAD(aad(clientId));
    const body = Buffer.concat([cipher.update(token, "utf8"), cipher.final()]);
    return `emu1:${Buffer.concat([iv, cipher.getAuthTag(), body]).toString("base64")}`;
  }

  async decrypt(ciphertext: string, clientId: string) {
    if (!ciphertext.startsWith("emu1:")) throw new Error("Not an emulator token ciphertext");
    const raw = Buffer.from(ciphertext.slice(5), "base64");
    const decipher = createDecipheriv("aes-256-gcm", this.key, raw.subarray(0, 12)).setAAD(aad(clientId));
    decipher.setAuthTag(raw.subarray(12, 28));
    return Buffer.concat([decipher.update(raw.subarray(28)), decipher.final()]).toString("utf8");
  }
}
