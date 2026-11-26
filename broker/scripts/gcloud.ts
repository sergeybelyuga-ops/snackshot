// Shared helpers for owner-only scripts. Runs gcloud with the `snackshot`
// configuration (personal account), never the machine default.
import { spawnSync } from "node:child_process";

export const GCLOUD_CONFIG = "snackshot";

export function gcloud(args: string[], input?: string): string {
  const all = [...args, `--configuration=${GCLOUD_CONFIG}`];
  const windows = process.platform === "win32";
  // gcloud is a .cmd wrapper on Windows, so it runs through cmd.exe, which splits unquoted
  // arguments at spaces. Quote every argument there.
  const res = spawnSync("gcloud", windows ? all.map(quoteForCmd) : all, {
    input,
    encoding: "utf8",
    shell: windows,
  });
  if (res.status !== 0) {
    // Drop the harmless Windows certificate-store warning gcloud prints.
    const err = (res.stderr ?? "")
      .split(/\r?\n/)
      .filter((l) => l && !/certificate|ssl\.py|warnings\.warn/i.test(l))
      .join("\n");
    throw new Error(`gcloud ${args.slice(0, 3).join(" ")} failed:\n${err}`);
  }
  return res.stdout;
}

function quoteForCmd(arg: string): string {
  if (/[%!]/.test(arg)) throw new Error(`Argument not supported on Windows: ${arg}`);
  return `"${arg.replace(/"/g, '""')}"`;
}

export function secretExists(project: string, name: string): boolean {
  try {
    gcloud(["secrets", "describe", name, `--project=${project}`, "--format=value(name)"]);
    return true;
  } catch {
    return false;
  }
}

/** Adds a new version of `name`, creating the secret if needed. The value goes through stdin only. */
export function putSecret(project: string, name: string, value: string): void {
  if (!secretExists(project, name)) {
    gcloud(["secrets", "create", name, `--project=${project}`, "--replication-policy=automatic"]);
  }
  gcloud(["secrets", "versions", "add", name, `--project=${project}`, "--data-file=-"], value);
}

export function readSecret(project: string, name: string): string {
  return gcloud(["secrets", "versions", "access", "latest", `--secret=${name}`, `--project=${project}`]).trim();
}

export function arg(name: string, fallback?: string): string {
  const i = process.argv.indexOf(`--${name}`);
  const v = i >= 0 ? process.argv[i + 1] : fallback;
  if (v === undefined) throw new Error(`Missing --${name}`);
  return v;
}
