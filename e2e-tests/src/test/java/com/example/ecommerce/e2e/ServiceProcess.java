package com.example.ecommerce.e2e;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Launches one Spring Boot service as a child JVM, tails its stdout/stderr,
 * and waits on the actuator health endpoint before returning.
 *
 * <p>Putting each service in its own JVM is what makes the E2E tractable:
 * three Spring Boot contexts in a single JVM kept colliding on Spring
 * Security defaults and Spring Cloud bootstrap. Separate JVMs share the
 * Testcontainers infrastructure over the host network but nothing else.
 */
public final class ServiceProcess implements AutoCloseable {

    private final String name;
    private final int port;
    private final Process process;
    private final Thread logPump;

    private ServiceProcess(String name, int port, Process process, Thread logPump) {
        this.name = name;
        this.port = port;
        this.process = process;
        this.logPump = logPump;
    }

    public static Builder named(String name) {
        return new Builder(name);
    }

    public int port() {
        return port;
    }

    public String name() {
        return name;
    }

    public String baseUrl() {
        return "http://localhost:" + port;
    }

    @Override
    public void close() {
        if (process.isAlive()) {
            process.destroy();
            try {
                if (!process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
        logPump.interrupt();
    }

    public static final class Builder {
        private final String name;
        private Path jarPath;
        private int port;
        private final List<String> args = new ArrayList<>();
        private String javaHome = System.getProperty("java.home");
        private Path logFile;

        private Builder(String name) {
            this.name = Objects.requireNonNull(name);
        }

        public Builder jar(Path jar) {
            this.jarPath = jar;
            return this;
        }

        public Builder javaHome(String javaHome) {
            this.javaHome = javaHome;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder logFile(Path logFile) {
            this.logFile = logFile;
            return this;
        }

        public Builder property(String key, String value) {
            args.add("--" + key + "=" + value);
            return this;
        }

        public ServiceProcess start() throws IOException, InterruptedException {
            Objects.requireNonNull(jarPath, "jar");
            if (port == 0) throw new IllegalStateException("port required");

            Path log = logFile != null
                    ? logFile
                    : Files.createTempFile("e2e-" + name + "-", ".log");

            List<String> cmd = new ArrayList<>();
            cmd.add(javaHome + "/bin/java");
            cmd.add("-Xmx384m");
            cmd.add("-jar");
            cmd.add(jarPath.toAbsolutePath().toString());
            cmd.add("--server.port=" + port);
            cmd.add("--management.endpoints.web.exposure.include=health");
            cmd.addAll(args);

            ProcessBuilder pb = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .redirectOutput(log.toFile());
            Process p = pb.start();

            Thread tail = new Thread(() -> tail(log, "[" + name + "]"));
            tail.setDaemon(true);
            tail.start();

            waitForHealth(port, Duration.ofMinutes(2), p, log);
            return new ServiceProcess(name, port, p, tail);
        }

        private static void waitForHealth(int port, Duration timeout, Process p, Path log)
                throws InterruptedException {
            Instant deadline = Instant.now().plus(timeout);
            URI healthUri = URI.create("http://localhost:" + port + "/actuator/health");
            while (Instant.now().isBefore(deadline)) {
                if (!p.isAlive()) {
                    throw new IllegalStateException(
                            "process died before becoming healthy; see log " + log);
                }
                if (probe(healthUri)) return;
                Thread.sleep(1000);
            }
            throw new IllegalStateException(
                    "timed out waiting for " + healthUri + "; see log " + log);
        }

        private static boolean probe(URI uri) {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) uri.toURL().openConnection();
                conn.setConnectTimeout(500);
                conn.setReadTimeout(1500);
                conn.setRequestMethod("GET");
                return conn.getResponseCode() == 200;
            } catch (IOException e) {
                return false;
            } finally {
                if (conn != null) conn.disconnect();
            }
        }

        private static void tail(Path log, String prefix) {
            try {
                // Wait for the file to exist
                while (!Files.exists(log) && !Thread.currentThread().isInterrupted()) {
                    Thread.sleep(100);
                }
                try (var reader = new BufferedReader(new InputStreamReader(Files.newInputStream(log)))) {
                    while (!Thread.currentThread().isInterrupted()) {
                        String line = reader.readLine();
                        if (line == null) {
                            Thread.sleep(200);
                            continue;
                        }
                        System.out.println(prefix + " " + line);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException ignored) {
            }
        }
    }
}
