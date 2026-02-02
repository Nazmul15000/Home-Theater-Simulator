import java.util.*;

/**
 * Main.java
 *
 * Single-file demo of a Home Theater system showcasing multiple design patterns:
 * Facade, Singleton, Builder, Command, Observer, State, Strategy, Memento.
 *
 * Put this file in IntelliJ (or any Java IDE), compile and run.
 *
 * Author: ChatGPT (example code)
 */
public class Main {
    public static void main(String[] args) {
        // Build a home theater using Builder
        HomeTheater theater = new HomeTheaterBuilder()
                .withAmplifier(new Amplifier("Yamaha Amp"))
                .withStreamingPlayer(new StreamingPlayer("Roku Ultra"))
                .withProjector(new Projector("Epson 4K", new WidescreenMode()))
                .withScreen(new Screen("Motorized Screen"))
                .withLights(new TheaterLights("Main Lights"))
                .withPopcornPopper(new PopcornPopper("Cuisinart Popper"))
                .build();

        // Create a facade to simplify usage
        HomeTheaterFacade facade = new HomeTheaterFacade(theater);

        // Create (singleton) remote and register some commands
        RemoteControl remote = RemoteControl.getInstance();

        // Commands
        remote.setCommand(0, new WatchMovieCommand(facade, "The Matrix (4K)"));
        remote.setCommand(1, new EndMovieCommand(facade));
        remote.setCommand(2, new PauseMovieCommand(facade));
        remote.setCommand(3, new ResumeMovieCommand(facade));
        remote.setCommand(4, new PopcornOnCommand(theater.getPopper()));
        remote.setCommand(5, new ScreenDownCommand(theater.getScreen()));

        // Demo observer: register a listener to streaming player events
        theater.getPlayer().addListener(new PlayerEventListener() {
            @Override
            public void onPlay(String movie) {
                System.out.println("[Observer] Player started: " + movie);
            }

            @Override
            public void onPause(String movie) {
                System.out.println("[Observer] Player paused: " + movie);
            }

            @Override
            public void onStop(String movie) {
                System.out.println("[Observer] Player stopped: " + movie);
            }
        });

        // Demo run sequence (simulate remote button presses)
        System.out.println(">>> Pressing remote slot 0 (watch movie)...");
        remote.onButtonPressed(0);

        // Simulate changing projector mode (Strategy)
        System.out.println(">>> Switching projector to Standard mode...");
        theater.getProjector().setMode(new StandardMode());
        theater.getProjector().displayInfo();

        // Pause and resume using commands
        System.out.println(">>> Pressing remote slot 2 (pause)...");
        remote.onButtonPressed(2);

        System.out.println(">>> Pressing remote slot 3 (resume)...");
        remote.onButtonPressed(3);

        // Popcorn
        System.out.println(">>> Pressing remote slot 4 (popcorn)...");
        remote.onButtonPressed(4);

        // Save player state (Memento) and then end movie
        System.out.println(">>> Saving player state and ending movie...");
        StreamingPlayer player = theater.getPlayer();
        PlayerMemento m = player.save(); // memento
        remote.onButtonPressed(1); // end movie

        // Undo: restore memento to resume where left off
        System.out.println(">>> Restoring saved player state (memento) and resuming...");
        player.restore(m);

        // Demonstrate remote undo (Command undo)
        System.out.println(">>> Pressing remote slot 5 (screen down) then undo...");
        remote.onButtonPressed(5);
        remote.undoButtonPressed();

        System.out.println(">>> Sequence finished.");
    }

    // ---------------------------
    // Builder and HomeTheater
    // ---------------------------
    static class HomeTheater {
        private Amplifier amp;
        private StreamingPlayer player;
        private Projector projector;
        private Screen screen;
        private TheaterLights lights;
        private PopcornPopper popper;

        public HomeTheater(Amplifier amp, StreamingPlayer player, Projector projector, Screen screen, TheaterLights lights, PopcornPopper popper) {
            this.amp = amp;
            this.player = player;
            this.projector = projector;
            this.screen = screen;
            this.lights = lights;
            this.popper = popper;
        }

        public Amplifier getAmp() { return amp; }
        public StreamingPlayer getPlayer() { return player; }
        public Projector getProjector() { return projector; }
        public Screen getScreen() { return screen; }
        public TheaterLights getLights() { return lights; }
        public PopcornPopper getPopper() { return popper; }
    }

    static class HomeTheaterBuilder {
        private Amplifier amp;
        private StreamingPlayer player;
        private Projector projector;
        private Screen screen;
        private TheaterLights lights;
        private PopcornPopper popper;

        public HomeTheaterBuilder withAmplifier(Amplifier amp) { this.amp = amp; return this; }
        public HomeTheaterBuilder withStreamingPlayer(StreamingPlayer player) { this.player = player; return this; }
        public HomeTheaterBuilder withProjector(Projector projector) { this.projector = projector; return this; }
        public HomeTheaterBuilder withScreen(Screen screen) { this.screen = screen; return this; }
        public HomeTheaterBuilder withLights(TheaterLights lights) { this.lights = lights; return this; }
        public HomeTheaterBuilder withPopcornPopper(PopcornPopper popper) { this.popper = popper; return this; }

        public HomeTheater build() {
            // Provide defaults for missing components
            if (amp == null) amp = new Amplifier("Default Amp");
            if (player == null) player = new StreamingPlayer("Default Player");
            if (projector == null) projector = new Projector("Default Projector", new WidescreenMode());
            if (screen == null) screen = new Screen("Default Screen");
            if (lights == null) lights = new TheaterLights("Default Lights");
            if (popper == null) popper = new PopcornPopper("Default Popper");
            return new HomeTheater(amp, player, projector, screen, lights, popper);
        }
    }

    // ---------------------------
    // Facade
    // ---------------------------
    static class HomeTheaterFacade {
        private HomeTheater theater;
        public HomeTheaterFacade(HomeTheater theater) { this.theater = theater; }

        public void watchMovie(String movie) {
            System.out.println("Facade: Get ready to watch a movie...");
            theater.getPopper().on();
            theater.getPopper().pop();
            theater.getLights().dim(20);
            theater.getScreen().down();
            theater.getProjector().on();
            theater.getProjector().wideScreenMode();
            theater.getAmp().on();
            theater.getAmp().setStreamingPlayer(theater.getPlayer());
            theater.getPlayer().play(movie);
        }

        public void endMovie() {
            System.out.println("Facade: Shutting movie theater down...");
            theater.getPopper().off();
            theater.getLights().on();
            theater.getScreen().up();
            theater.getProjector().off();
            theater.getAmp().off();
            theater.getPlayer().stop();
        }

        public void pauseMovie() {
            theater.getPlayer().pause();
            theater.getLights().dim(50);
        }

        public void resumeMovie() {
            theater.getPlayer().resume();
            theater.getLights().dim(10);
        }
    }

    // ---------------------------
    // Command Pattern
    // ---------------------------
    interface Command {
        void execute();
        void undo();
    }

    static class RemoteControl {
        private static RemoteControl instance;
        private Command[] slots = new Command[10];
        private Command lastCommand;

        private RemoteControl() {}

        public static RemoteControl getInstance() {
            if (instance == null) instance = new RemoteControl();
            return instance;
        }

        public void setCommand(int slot, Command command) {
            if (slot < 0 || slot >= slots.length) throw new IllegalArgumentException("Invalid slot");
            slots[slot] = command;
        }

        public void onButtonPressed(int slot) {
            Command cmd = slots[slot];
            if (cmd == null) {
                System.out.println("Remote: no command assigned to slot " + slot);
                return;
            }
            cmd.execute();
            lastCommand = cmd;
        }

        public void undoButtonPressed() {
            if (lastCommand != null) {
                lastCommand.undo();
                lastCommand = null;
            } else {
                System.out.println("Remote: nothing to undo");
            }
        }
    }

    static class WatchMovieCommand implements Command {
        private HomeTheaterFacade facade;
        private String movie;
        public WatchMovieCommand(HomeTheaterFacade f, String movie) { this.facade = f; this.movie = movie; }
        @Override public void execute() { facade.watchMovie(movie); }
        @Override public void undo() { facade.endMovie(); }
    }

    static class EndMovieCommand implements Command {
        private HomeTheaterFacade facade;
        public EndMovieCommand(HomeTheaterFacade f) { this.facade = f; }
        @Override public void execute() { facade.endMovie(); }
        @Override public void undo() { /* not easily undone */ System.out.println("Cannot undo endMovie"); }
    }

    static class PauseMovieCommand implements Command {
        private HomeTheaterFacade facade;
        public PauseMovieCommand(HomeTheaterFacade f) { this.facade = f; }
        @Override public void execute() { facade.pauseMovie(); }
        @Override public void undo() { facade.resumeMovie(); }
    }

    static class ResumeMovieCommand implements Command {
        private HomeTheaterFacade facade;
        public ResumeMovieCommand(HomeTheaterFacade f) { this.facade = f; }
        @Override public void execute() { facade.resumeMovie(); }
        @Override public void undo() { facade.pauseMovie(); }
    }

    static class PopcornOnCommand implements Command {
        private PopcornPopper popper;
        public PopcornOnCommand(PopcornPopper p) { this.popper = p; }
        @Override public void execute() { popper.on(); popper.pop(); }
        @Override public void undo() { popper.off(); }
    }

    static class ScreenDownCommand implements Command {
        private Screen screen;
        public ScreenDownCommand(Screen s) { this.screen = s; }
        @Override public void execute() { screen.down(); }
        @Override public void undo() { screen.up(); }
    }

    // ---------------------------
    // Observer Pattern (Player events)
    // ---------------------------
    interface PlayerEventListener {
        void onPlay(String movie);
        void onPause(String movie);
        void onStop(String movie);
    }

    // ---------------------------
    // Memento for player
    // ---------------------------
    static class PlayerMemento {
        private final String movie;
        private final int position;
        private final StreamingPlayer.StateEnum state;

        public PlayerMemento(String movie, int position, StreamingPlayer.StateEnum state) {
            this.movie = movie;
            this.position = position;
            this.state = state;
        }
    }

    // ---------------------------
    // Streaming Player with State and Memento
    // ---------------------------
    static class StreamingPlayer {
        private String name;
        private String currentMovie;
        private int positionSeconds = 0;
        private State stateObj;
        private List<PlayerEventListener> listeners = new ArrayList<>();

        // Enum for memento/state snapshot
        enum StateEnum {STOPPED, PLAYING, PAUSED}

        public StreamingPlayer(String name) {
            this.name = name;
            this.stateObj = new StoppedState(this);
        }

        // Observer
        public void addListener(PlayerEventListener listener) { listeners.add(listener); }
        public void removeListener(PlayerEventListener listener) { listeners.remove(listener); }

        // State transitions
        public void play(String movie) { stateObj.play(movie); }
        public void stop() { stateObj.stop(); }
        public void pause() { stateObj.pause(); }
        public void resume() { stateObj.resume(); }

        public String getName() { return name; }
        public String getCurrentMovie() { return currentMovie; }
        public void setCurrentMovie(String m) { this.currentMovie = m; }
        public int getPositionSeconds() { return positionSeconds; }
        public void setPositionSeconds(int s) { this.positionSeconds = s; }

        // notify observers
        void notifyPlay(String movie) { for (PlayerEventListener l : listeners) l.onPlay(movie); }
        void notifyPause(String movie) { for (PlayerEventListener l : listeners) l.onPause(movie); }
        void notifyStop(String movie) { for (PlayerEventListener l : listeners) l.onStop(movie); }

        // state object setter
        void setState(State s) { this.stateObj = s; }

        // Memento
        public PlayerMemento save() {
            StateEnum st = StateEnum.STOPPED;
            if (stateObj instanceof PlayingState) st = StateEnum.PLAYING;
            else if (stateObj instanceof PausedState) st = StateEnum.PAUSED;
            System.out.println("[Memento] Saving state: movie=" + currentMovie + " pos=" + positionSeconds + " state=" + st);
            return new PlayerMemento(currentMovie, positionSeconds, st);
        }

        public void restore(PlayerMemento m) {
            System.out.println("[Memento] Restoring state...");
            if (m == null) return;
            this.currentMovie = m.movie;
            this.positionSeconds = m.position;
            switch (m.state) {
                case PLAYING:
                    setState(new PlayingState(this));
                    notifyPlay(currentMovie);
                    break;
                case PAUSED:
                    setState(new PausedState(this));
                    notifyPause(currentMovie);
                    break;
                default:
                    setState(new StoppedState(this));
                    notifyStop(currentMovie);
            }
            System.out.println("[Memento] Restored: movie=" + currentMovie + " pos=" + positionSeconds + " state=" + m.state);
        }

        // State interface & concrete states
        interface State {
            void play(String movie);
            void stop();
            void pause();
            void resume();
        }

        static class StoppedState implements State {
            private StreamingPlayer player;
            public StoppedState(StreamingPlayer p) { this.player = p; }
            @Override public void play(String movie) {
                player.setCurrentMovie(movie);
                player.setPositionSeconds(0);
                player.setState(new PlayingState(player));
                System.out.println(player.getName() + " now playing: " + movie);
                player.notifyPlay(movie);
            }
            @Override public void stop() { System.out.println(player.getName() + " already stopped."); player.notifyStop(player.getCurrentMovie()); }
            @Override public void pause() { System.out.println("Cannot pause - player is stopped."); }
            @Override public void resume() { System.out.println("Cannot resume - player is stopped."); }
        }

        static class PlayingState implements State {
            private StreamingPlayer player;
            public PlayingState(StreamingPlayer p) { this.player = p; }
            @Override public void play(String movie) {
                System.out.println("Already playing. Switching to: " + movie);
                player.setCurrentMovie(movie);
                player.setPositionSeconds(0);
                player.notifyPlay(movie);
            }
            @Override public void stop() {
                System.out.println(player.getName() + " stopping...");
                player.setState(new StoppedState(player));
                player.notifyStop(player.getCurrentMovie());
            }
            @Override public void pause() {
                System.out.println(player.getName() + " pausing...");
                player.setState(new PausedState(player));
                player.notifyPause(player.getCurrentMovie());
            }
            @Override public void resume() { System.out.println("Already playing."); }
        }

        static class PausedState implements State {
            private StreamingPlayer player;
            public PausedState(StreamingPlayer p) { this.player = p; }
            @Override public void play(String movie) {
                System.out.println("From paused: playing " + movie);
                player.setCurrentMovie(movie);
                player.setPositionSeconds(0);
                player.setState(new PlayingState(player));
                player.notifyPlay(movie);
            }
            @Override public void stop() {
                System.out.println("From paused: stopping...");
                player.setState(new StoppedState(player));
                player.notifyStop(player.getCurrentMovie());
            }
            @Override public void pause() { System.out.println("Already paused."); }
            @Override public void resume() {
                System.out.println(player.getName() + " resuming...");
                player.setState(new PlayingState(player));
                player.notifyPlay(player.getCurrentMovie());
            }
        }
    }

    // ---------------------------
    // Projector + Strategy (Video modes)
    // ---------------------------
    interface VideoMode {
        String getName();
        void applyMode(Projector p);
    }

    static class WidescreenMode implements VideoMode {
        @Override public String getName() { return "WIDESCREEN (2.39:1)"; }
        @Override public void applyMode(Projector p) { System.out.println(p.getName() + " set to widescreen mode. Letterboxing applied."); }
    }

    static class StandardMode implements VideoMode {
        @Override public String getName() { return "STANDARD (16:9)"; }
        @Override public void applyMode(Projector p) { System.out.println(p.getName() + " set to standard 16:9 mode."); }
    }

    static class Projector {
        private String name;
        private VideoMode mode;
        public Projector(String name, VideoMode mode) { this.name = name; this.mode = mode; }
        public String getName() { return name; }
        public void on() { System.out.println(name + " powered on."); }
        public void off() { System.out.println(name + " powered off."); }
        public void wideScreenMode() { setMode(new WidescreenMode()); }
        public void setMode(VideoMode mode) { this.mode = mode; this.mode.applyMode(this); }
        public void displayInfo() { System.out.println("[Projector] " + name + " mode=" + mode.getName()); }
    }

    // ---------------------------
    // Screen
    // ---------------------------
    static class Screen {
        private String name;
        private boolean isDown = false;
        public Screen(String name) { this.name = name; }
        public void down() { if (!isDown) { isDown = true; System.out.println(name + " lowered."); } else System.out.println(name + " already down."); }
        public void up() { if (isDown) { isDown = false; System.out.println(name + " raised."); } else System.out.println(name + " already up."); }
    }

    // ---------------------------
    // Lights
    // ---------------------------
    static class TheaterLights {
        private String name;
        public TheaterLights(String name) { this.name = name; }
        public void dim(int percent) { System.out.println(name + " dimming to " + percent + "%"); }
        public void on() { System.out.println(name + " lights on."); }
        public void off() { System.out.println(name + " lights off."); }
    }

    // ---------------------------
    // Popcorn Popper
    // ---------------------------
    static class PopcornPopper {
        private String name;
        private boolean isOn = false;
        public PopcornPopper(String name) { this.name = name; }
        public void on() { isOn = true; System.out.println(name + " on."); }
        public void off() { isOn = false; System.out.println(name + " off."); }
        public void pop() { if (isOn) System.out.println(name + " popping popcorn! 🍿"); else System.out.println(name + " is off - cannot pop."); }
    }

    // ---------------------------
    // Amplifier
    // ---------------------------
    static class Amplifier {
        private String name;
        private StreamingPlayer streamingPlayer;
        public Amplifier(String name) { this.name = name; }
        public void on() { System.out.println(name + " on."); }
        public void off() { System.out.println(name + " off."); }
        public void setStreamingPlayer(StreamingPlayer p) { this.streamingPlayer = p; System.out.println(name + " connected to player " + p.getName()); }
    }

    // ---------------------------
    // Singleton Config example
    // ---------------------------
    static class TheaterConfig {
        private static TheaterConfig instance;
        private Map<String, String> config = new HashMap<>();
        private TheaterConfig() { config.put("defaultVolume", "50"); }
        public static TheaterConfig getInstance() { if (instance == null) instance = new TheaterConfig(); return instance; }
        public String get(String key) { return config.get(key); }
        public void set(String key, String val) { config.put(key, val); }
    }
}
