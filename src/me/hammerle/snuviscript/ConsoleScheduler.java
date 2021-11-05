package me.hammerle.snuviscript;

import java.util.ArrayList;
import me.hammerle.snuviscript.code.ISnuviScheduler;

public class ConsoleScheduler implements ISnuviScheduler {
    private class Task {
        private Runnable r;
        private long delay;

        public Task(Runnable r, long delay) {
            this.r = r;
            this.delay = delay;
        }

        public void tick() {
            delay--;
            if(delay <= 0 && r != null) {
                r.run();
                r = null;
                activeTasks--;
            }
        }

        public void set(Runnable r, long delay) {
            this.r = r;
            this.delay = delay;
        }

        public boolean isFree() {
            return r == null;
        }
    }

    private int activeTasks = 0;
    private final ArrayList<Task> tasks = new ArrayList<>();

    @Override
    public void scheduleTask(String name, Runnable r, long delay) {
        activeTasks++;
        for(int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            if(t.isFree()) {
                t.set(r, delay);
                return;
            }
        }
        tasks.add(new Task(r, delay));
    }

    public void tick() {
        while(activeTasks > 0) {
            for(int i = 0; i < tasks.size(); i++) {
                tasks.get(i).tick();
            }
        }
    }
}
