# Units

Two services, one per environment, each with its own env file and its own jar.

`Restart=always` means **you never start the app by hand.** To restart it, kill the PID and
let systemd bring it back:

```bash
kill $(systemctl show app.service --property=MainPID --value)
```

Starting a second copy manually is how you get two processes fighting over one port, one
database advisory lock, or one API credential — and the manual one has none of the unit's
environment, so it fails in ways that look like application bugs.

`SuccessExitStatus=143` is there because 143 is a clean SIGTERM shutdown; without it every
normal restart is logged as a failure.

Shutdown is graceful and can take 10–15 seconds. Poll for a new PID and an HTTP 200 before
concluding a deploy failed.

## Running a newer JDK than the system one

Rather than editing the unit, drop in an override — it survives a redeploy of the unit file
and is one file to delete when rolling back:

```ini
# /etc/systemd/system/app.service.d/jdk25.conf
[Service]
ExecStart=
ExecStart=/home/app/dev/.jdks/jdk-25/bin/java -jar /home/app/dev/app/deploy/app-prod.jar
```

The empty `ExecStart=` is required: without it systemd appends rather than replaces.
