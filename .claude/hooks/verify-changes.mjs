import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';

const input = JSON.parse(fs.readFileSync(0, 'utf8'));
const filePath = input.tool_input?.file_path ?? '';
const projectDir = process.env.CLAUDE_PROJECT_DIR || process.cwd();

function runCheck(cmd, args, cwd) {
  try {
    execFileSync(cmd, args, {
      cwd,
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'pipe'],
    });
  } catch (err) {
    const output = [err.stdout, err.stderr].filter(Boolean).join('\n');
    process.stderr.write(`検証エラー (${cmd} ${args.join(' ')}):\n${output}\n`);
    process.exit(2);
  }
}

if (filePath.endsWith('.kt')) {
  const gradlew = process.platform === 'win32' ? 'gradlew.bat' : './gradlew';
  runCheck(gradlew, ['cleanTest', 'test', '--tests', '*ArchitectureTest'], path.join(projectDir, 'backend'));
} else if (filePath.endsWith('.ts') || filePath.endsWith('.tsx')) {
  runCheck('pnpm', ['exec', 'tsc', '--noEmit'], path.join(projectDir, 'frontend'));
}
