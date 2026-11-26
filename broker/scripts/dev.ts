// Local broker loop (docs/Architecture/12_dev-and-test-environment.md §10.2):
// the fake LogMeal server on :8090 plus the emulators (project demo-snackshot).
// Ctrl+C stops both. Seed in another terminal with `npm run seed`.
//
//   cd broker; npm run dev [-- --scenario 429]
import { spawn } from "node:child_process";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { startFakeLogMeal } from "../../tools/fake-logmeal/server.ts";

const broker = join(dirname(fileURLToPath(import.meta.url)), "..");
const i = process.argv.indexOf("--scenario");
const fake = await startFakeLogMeal({ port: 8090, scenario: i > 0 ? (process.argv[i + 1] ?? null) : null });
console.log(`Fake LogMeal on ${fake.url}`);

await new Promise<void>((resolve, reject) => {
  const build = spawn("npm", ["--prefix", "functions", "run", "build"], { cwd: broker, stdio: "inherit", shell: true });
  build.on("exit", (code) => (code === 0 ? resolve() : reject(new Error(`build exited with ${code}`))));
});

const emulators = spawn("npx", ["firebase", "emulators:start", "--project", "demo-snackshot"], { cwd: broker, stdio: "inherit", shell: true });
const stop = async () => {
  emulators.kill("SIGINT");
  await fake.close();
};
process.on("SIGINT", stop);
emulators.on("exit", async (code) => {
  await fake.close().catch(() => undefined);
  process.exit(code ?? 0);
});
