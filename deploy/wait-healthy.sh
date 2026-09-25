# Sourced by the deploy scripts. Restart a service and wait until the NEW process answers
# /api/health — a new PID alone is not proof, and a 200 from something other than the backend
# (a frontend catching the path) once looked exactly like success.
restart_and_wait() {
  local service="$1" port="$2" timeout="${3:-120}"
  local old new deadline
  old=$(systemctl show "$service" --property=MainPID --value)
  # MainPID is 0 when the service is not running — and `kill 0` signals this whole process
  # group, the deploy script included. Refuse instead.
  if [[ -z "$old" || "$old" == "0" ]]; then
    echo "!! $service is not running — start it first (systemctl start $service)" >&2
    return 1
  fi
  kill "$old"
  deadline=$((SECONDS + timeout))
  echo -n "waiting for $service"
  while (( SECONDS < deadline )); do
    new=$(systemctl show "$service" --property=MainPID --value)
    if [[ "$new" != "$old" && "$new" != "0" ]] \
       && curl -fsS "http://127.0.0.1:$port/api/health" 2>/dev/null | grep -q '"ok"'; then
      echo " — up (pid $new)"
      return 0
    fi
    echo -n "."; sleep 3
  done
  echo
  echo "!! $service did not report healthy within ${timeout}s. Check: journalctl -u $service -n 80" >&2
  return 1
}
