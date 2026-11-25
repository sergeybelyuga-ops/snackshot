// Owner-only. Stores a secret typed at a hidden prompt in Secret Manager.
// The value never touches disk, shell history, or the terminal.
//
//   node broker/scripts/put-secret.ts --project snackshot-prod --name LOGMEAL_COMPANY_TOKEN
import { arg, putSecret } from "./gcloud.ts";

function promptHidden(question: string): Promise<string> {
  return new Promise((resolve, reject) => {
    const stdin = process.stdin;
    if (!stdin.isTTY) return reject(new Error("Run this in an interactive terminal."));
    process.stdout.write(question);
    stdin.setRawMode(true);
    stdin.resume();
    stdin.setEncoding("utf8");
    let value = "";
    const onData = (chunk: string) => {
      for (const ch of chunk) {
        if (ch === "\r" || ch === "\n") {
          stdin.setRawMode(false);
          stdin.pause();
          stdin.off("data", onData);
          process.stdout.write("\n");
          return resolve(value.trim());
        }
        if (ch === "\u0003") {
          process.stdout.write("\n");
          process.exit(130); // Ctrl+C
        }
        if (ch === "\u0008" || ch === "\u007f") {
          value = value.slice(0, -1);
        } else {
          value += ch;
        }
      }
    };
    stdin.on("data", onData);
  });
}

const project = arg("project");
const name = arg("name");
const value = await promptHidden(`Value for ${name} in ${project} (input hidden): `);
if (value.length < 10) throw new Error("Value looks too short; nothing stored.");
putSecret(project, name, value);
console.log(`Stored a new version of ${name} in ${project} (${value.length} characters).`);
