package github.kasuminova.network.message.whitelist;

import github.kasuminova.serverhelperbc.ServerHelperBC;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class UserInGroupQueryMessage {

    public long id;

    public UserInGroupQueryMessage(final long id) {
        this.id = id;
    }

    public static boolean query(final long id) {
        var managers = ServerHelperBC.CONNECTED_MANAGERS;
        if (managers.isEmpty()) {
            return true;
        }

        for (ChannelHandlerContext ctx : managers.values()) {
            ctx.writeAndFlush(new UserInGroupQueryMessage(id));
        }

        return BlockingHandler.query(id);
    }

    public static class BlockingHandler {

        private static final Map<Long, Task> TASK_MAP = new ConcurrentHashMap<>();

        private static boolean query(final long id) {
            BlockingHandler.submit(id);
            return TASK_MAP.get(id).anySuccess();
        }

        public static void completePartially(final long id, final boolean result) {
            var task = TASK_MAP.get(id);
            if (task == null) {
                return;
            }
            task.futures.stream()
                        .filter(future -> !future.isDone())
                        .findAny()
                        .ifPresent(future -> future.complete(result));
        }

        private static void submit(final long id) {
            List<CompletableFuture<Boolean>> tasks = new ArrayList<>();
            for (final var __ : ServerHelperBC.CONNECTED_MANAGERS.values()) {
                tasks.add(new CompletableFuture<>());
            }
            TASK_MAP.put(id, new Task(tasks));
        }

        private record Task(List<CompletableFuture<Boolean>> futures) {

            public boolean anySuccess() {
                boolean anyTimeOut = false;

                for (CompletableFuture<Boolean> future : futures) {
                    try {
                        if (future.get(5000L, TimeUnit.MILLISECONDS)) {
                            return true;
                        }
                    } catch (InterruptedException | ExecutionException ignored) {
                    } catch (TimeoutException e) {
                        anyTimeOut = true;
                    }
                }

                return anyTimeOut;
            }

        }

    }
}
