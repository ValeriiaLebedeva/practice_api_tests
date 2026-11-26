#!/usr/bin/env bash
# Прогоняет тесты iteration2 (API и UI) несколько раз подряд и собирает подтверждение.
# Запуск из корня проекта:  bash run10.sh
# Другое число прогонов:    RUNS=5 bash run10.sh
#
# Перед запуском должны работать приложение, бэкенд и Selenoid.
# Результат: папка run-results (логи, отчёты по прогонам, summary.csv и summary.md).

RUNS="${RUNS:-10}"
TEST_PATTERN="${TEST_PATTERN:-api.iteration2.*Test,ui.iteration2.*Test}"
OUT=run-results

# --- где взять Maven: переменная MVN, затем mvn в PATH, затем mvnw, затем Maven из IntelliJ ---
MVN="${MVN:-}"
if [ -z "$MVN" ]; then
  if command -v mvn >/dev/null 2>&1; then
    MVN=mvn
  elif [ -x ./mvnw ]; then
    MVN=./mvnw
  else
    for candidate in /Applications/IntelliJ*.app/Contents/plugins/maven/lib/maven3/bin/mvn \
                     "$HOME"/Applications/IntelliJ*.app/Contents/plugins/maven/lib/maven3/bin/mvn; do
      if [ -x "$candidate" ]; then MVN="$candidate"; break; fi
    done
  fi
fi
if [ -z "$MVN" ]; then
  echo "Maven не найден. Установите его (brew install maven) или укажите путь:"
  echo "  MVN=/путь/к/mvn bash run10.sh"
  exit 1
fi
echo "Maven: $MVN"

# --- Java 21 (на ней работает проект), если она установлена ---
if [ -x /usr/libexec/java_home ] && JH=$(/usr/libexec/java_home -v 21 2>/dev/null); then
  export JAVA_HOME="$JH"
fi
echo "JAVA_HOME: ${JAVA_HOME:-не задан}"

rm -rf "$OUT"
mkdir -p "$OUT"
echo "run,maven_exit_code" > "$OUT/exit-codes.csv"

for i in $(seq 1 "$RUNS"); do
  echo "=== Прогон $i из $RUNS ($(date '+%H:%M:%S')) ==="
  # чистим старые отчёты, чтобы в прогон не попали результаты других классов
  rm -rf target/surefire-reports
  "$MVN" test -Dtest="$TEST_PATTERN" -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | tee "$OUT/run-$i.log"
  code=${PIPESTATUS[0]}
  echo "$i,$code" >> "$OUT/exit-codes.csv"
  if [ "$code" = "127" ]; then echo "Maven не запустился (код 127), прогоны остановлены."; exit 1; fi
  mkdir -p "$OUT/run-$i"
  cp -R target/surefire-reports/. "$OUT/run-$i/" 2>/dev/null
done

python3 - "$RUNS" <<'PYEOF'
import csv, glob, sys, xml.etree.ElementTree as ET

runs = int(sys.argv[1])
codes = {}
for row in csv.DictReader(open("run-results/exit-codes.csv")):
    codes[int(row["run"])] = int(row["maven_exit_code"])

rows = []
for i in range(1, runs + 1):
    tests = failures = errors = skipped = 0
    for path in glob.glob(f"run-results/run-{i}/TEST-*.xml"):
        root = ET.parse(path).getroot()
        tests += int(root.get("tests"))
        failures += int(root.get("failures"))
        errors += int(root.get("errors"))
        skipped += int(root.get("skipped"))
    green = tests > 0 and failures == 0 and errors == 0 and codes.get(i) == 0
    rows.append((i, tests, failures, errors, skipped, codes.get(i), "ЗЕЛЁНЫЙ" if green else "ЕСТЬ ПАДЕНИЯ"))

header = ("run", "tests", "failures", "errors", "skipped", "maven_exit_code", "result")
csv.writer(open("run-results/summary.csv", "w", newline="")).writerows([header, *rows])

with open("run-results/summary.md", "w") as md:
    md.write("| Прогон | Тестов | Падений | Ошибок | Пропущено | Код Maven | Итог |\n|---|---|---|---|---|---|---|\n")
    for r in rows:
        md.write("| " + " | ".join(map(str, r)) + " |\n")
    green_runs = sum(1 for r in rows if r[-1] == "ЗЕЛЁНЫЙ")
    md.write(f"\nЗелёных прогонов: {green_runs} из {runs}. "
             "Пропущенные тесты (skipped) это @Disabled, а не падения.\n")

print()
print(open("run-results/summary.md").read())
PYEOF
