package puregero.multipaper.server;

import io.netty.channel.ChannelFuture;
import puregero.multipaper.mastermessagingprotocol.MessageBootstrap;
import puregero.multipaper.mastermessagingprotocol.messages.masterbound.MasterBoundMessage;
import puregero.multipaper.mastermessagingprotocol.messages.masterbound.MasterBoundProtocol;
import puregero.multipaper.mastermessagingprotocol.messages.serverbound.ServerBoundMessage;
import puregero.multipaper.mastermessagingprotocol.messages.serverbound.ServerBoundProtocol;
import puregero.multipaper.server.proxy.ProxyServer;
import puregero.multipaper.server.util.LogToFile;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class MultiPaperServer extends MessageBootstrap<MasterBoundMessage, ServerBoundMessage> {
    public static final int DEFAULT_PORT = 35353;
    public static final String SECRET = UUID.randomUUID().toString();

    public static void main(String[] args) throws InterruptedException {
        DAEMON = false;

        String address = null;
        int port = DEFAULT_PORT;

        if ("true".equalsIgnoreCase(System.getProperty("logging.enabled", "true"))) {
            LogToFile.init(); // TODO Use log4j instead
        }

        if (args.length > 0) {
            if (args[0].contains(":")) {
                address = args[0].substring(0, args[0].indexOf(':'));
                args[0] = args[0].substring(args[0].indexOf(':') + 1);
            }
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Usage: java -jar MultiPaperServer.jar <[address:]port> [proxy port]");
                System.exit(1);
            }
        }

        if (args.length > 1) {
            try {
                setupGracefulShutdown();
                ProxyServer.openServer(Integer.parseInt(args[1]));
            } catch (NumberFormatException e) {
                System.err.println("Usage: java -jar MultiPaperServer.jar <[address:]port> [proxy port]");
                System.exit(1);
            }
        }

        new MultiPaperServer(address, port);

        if (new CommandLineInput().run()) {
            System.exit(0);
        }
    }

    private static void awaitAsyncTasks() {
        System.out.println("Waiting for async tasks...");
        if (!ForkJoinPool.commonPool().awaitQuiescence(10, TimeUnit.SECONDS)) {
            System.out.println("Some tasks are taking a long time to complete.");
            System.out.println("Thread list:");
            ThreadInfo[] threads = ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);
            for (ThreadInfo thread : threads) {
                System.out.println(thread);
            }

            ForkJoinPool.commonPool().awaitQuiescence(1, TimeUnit.HOURS);
        }
    }

    public MultiPaperServer(int port) {
        this(null, port);
    }

    public MultiPaperServer(String address, int port) {
        super(new MasterBoundProtocol(), new ServerBoundProtocol(), channel -> channel.pipeline().addLast(new ServerConnection(channel)));

        ChannelFuture future;
        if (address == null) {
            future = this.listenOn(port);
        } else {
            future = this.listenOn(address, port);
        }

        future.addListener(f -> {
            if (f.cause() != null) {
                f.cause().printStackTrace();
            } else {
                System.out.println("[MultiPaperMaster] Listening on " + (address == null ? "0.0.0.0" : address) + ":" + port);
            }
        });
    }

    private static void setupGracefulShutdown() {
        var shutdownListener = new Thread(() -> {
            System.out.println("Exiting safely...");

            awaitAsyncTasks();

            System.out.println("Shutting down event loop...");
            try {
                eventLoopGroup.shutdownGracefully().await();
            } catch (InterruptedException e) {
                throw new RuntimeException("Error while waiting event loop group shutdown", e);
            }

            awaitAsyncTasks();
        });
        Runtime.getRuntime().addShutdownHook(shutdownListener);
    }
}
