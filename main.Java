// IdoLab - Pixel-art inspired management sim prototype
// Bright, poppy visual prototype using Java Swing/Java2D. Tap/drag with mouse.
// Features: CEO/Trainee modes, weekly drag-and-drop activities, survival show rounds,
// debut flow, pixel-art friendly rendering, simple save/load.

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class Main extends JPanel implements MouseListener, MouseMotionListener {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("IdoLab Pixel Sim");
            Main main = new Main();
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setContentPane(main);
            frame.setSize(1024, 720);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            main.start();
        });
    }

    private final Random rng = new Random();
    private final GameState state = new GameState();
    private final List<Room> rooms = new ArrayList<>();
    private TraineeAvatar dragging;
    private Point dragOffset = new Point();
    private long lastTick = System.currentTimeMillis();
    private boolean showSurvivalOverlay = false;

    public Main() {
        setBackground(new Color(240, 248, 255));
        setFocusable(true);
        initRooms();
        addMouseListener(this);
        addMouseMotionListener(this);
        setupKeybinds();
    }

    private void start() {
        Timer timer = new Timer(16, e -> updateAndRepaint());
        timer.start();
    }

    private void initRooms() {
        rooms.add(new Room("Dance Studio", ActivityType.DANCE, new Rectangle(40, 120, 200, 120), new Color(255, 210, 240)));
        rooms.add(new Room("Vocal Booth", ActivityType.VOCAL, new Rectangle(260, 120, 200, 120), new Color(210, 255, 230)));
        rooms.add(new Room("Rap Lab", ActivityType.RAP, new Rectangle(480, 120, 200, 120), new Color(220, 220, 255)));
        rooms.add(new Room("Gym", ActivityType.GYM, new Rectangle(700, 120, 200, 120), new Color(255, 240, 210)));

        rooms.add(new Room("Recording", ActivityType.RECORDING, new Rectangle(40, 280, 200, 120), new Color(250, 225, 180)));
        rooms.add(new Room("Media", ActivityType.MEDIA, new Rectangle(260, 280, 200, 120), new Color(235, 210, 255)));
        rooms.add(new Room("Rest", ActivityType.REST, new Rectangle(480, 280, 200, 120), new Color(210, 235, 255)));
        rooms.add(new Room("Special", ActivityType.SPECIAL, new Rectangle(700, 280, 200, 120), new Color(255, 235, 235)));

        rooms.add(new Room("Evaluation", ActivityType.EVALUATION, new Rectangle(40, 440, 280, 120), new Color(240, 255, 210)));
        rooms.add(new Room("Survival Show", ActivityType.SURVIVAL, new Rectangle(360, 440, 280, 120), new Color(255, 220, 200)));
        rooms.add(new Room("Debut Hall", ActivityType.DEBUT, new Rectangle(680, 440, 220, 120), new Color(220, 255, 240)));
    }

    private void setupKeybinds() {
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "save");
        actionMap.put("save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                state.save();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK), "load");
        actionMap.put("load", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                state.load();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "toggleMode");
        actionMap.put("toggleMode", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                state.toggleMode();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "showSurvival");
        actionMap.put("showSurvival", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showSurvivalOverlay = !showSurvivalOverlay;
            }
        });
    }

    private void updateAndRepaint() {
        long now = System.currentTimeMillis();
        float delta = (now - lastTick) / 1000f;
        lastTick = now;

        state.update(delta, rooms, showSurvivalOverlay ? ActivityType.SURVIVAL : null, rng);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        paintBackground(g2d);
        paintRooms(g2d);
        paintTrainees(g2d);
        paintHUD(g2d);

        if (showSurvivalOverlay) {
            paintSurvivalOverlay(g2d);
        }
    }

    private void paintBackground(Graphics2D g2d) {
        g2d.setColor(new Color(245, 245, 255));
        g2d.fillRect(0, 0, getWidth(), getHeight());
        g2d.setColor(new Color(255, 215, 245));
        g2d.fillRect(0, 80, getWidth(), 20);
        g2d.setColor(new Color(220, 255, 240));
        g2d.fillRect(0, 240, getWidth(), 20);
        g2d.setColor(new Color(255, 245, 220));
        g2d.fillRect(0, 400, getWidth(), 20);
    }

    private void paintRooms(Graphics2D g2d) {
        for (Room room : rooms) {
            g2d.setColor(room.color);
            g2d.fill(room.rect);
            g2d.setColor(new Color(0, 0, 0, 40));
            g2d.draw(room.rect);
            g2d.setColor(new Color(60, 60, 80));
            g2d.setFont(getFont().deriveFont(Font.BOLD, 14f));
            g2d.drawString(room.name, room.rect.x + 8, room.rect.y + 18);
            String desc = room.type.description;
            g2d.setFont(getFont().deriveFont(11f));
            g2d.drawString(desc, room.rect.x + 8, room.rect.y + room.rect.height - 8);
        }
    }

    private void paintTrainees(Graphics2D g2d) {
        for (TraineeAvatar avatar : state.avatars) {
            avatar.paint(g2d);
        }
    }

    private void paintHUD(Graphics2D g2d) {
        int padding = 12;
        g2d.setColor(new Color(40, 40, 60));
        g2d.setFont(getFont().deriveFont(Font.BOLD, 16f));
        g2d.drawString("Mode: " + state.mode.label + " (Space to toggle)", padding, 20);
        g2d.drawString("Week " + state.week + " / Day " + state.day + " | ₩" + state.companyMoney + " | Fans " + state.fans + " | Rep " + state.reputation, padding, 40);
        g2d.drawString("Ctrl+S Save | Ctrl+L Load | Tab Survival Show", padding, 60);

        int x = getWidth() - 220;
        int y = 16;
        g2d.setFont(getFont().deriveFont(Font.PLAIN, 12f));
        for (TraineeAvatar avatar : state.avatars) {
            g2d.setColor(new Color(0, 0, 0, 30));
            g2d.fillRoundRect(x - 10, y - 14, 200, 50, 12, 12);
            g2d.setColor(Color.WHITE);
            g2d.fillRoundRect(x - 12, y - 16, 200, 50, 12, 12);
            g2d.setColor(new Color(60, 60, 60));
            g2d.drawString(avatar.trainee.name + " ⭐" + avatar.trainee.starRating + " Pop " + avatar.trainee.popularity, x, y);
            g2d.drawString("V" + avatar.trainee.vocal + " D" + avatar.trainee.dance + " R" + avatar.trainee.rap + " Vis" + avatar.trainee.visual + " Cha" + avatar.trainee.charisma + " Sta" + avatar.trainee.stamina, x, y + 16);
            g2d.drawString("Activity: " + avatar.currentLabel(), x, y + 32);
            y += 60;
        }

        if (state.debutReadyPopup > 0) {
            g2d.setColor(new Color(0, 0, 0, 120));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setColor(Color.WHITE);
            g2d.fillRoundRect(getWidth() / 2 - 180, getHeight() / 2 - 80, 360, 160, 20, 20);
            g2d.setColor(new Color(40, 40, 60));
            g2d.setFont(getFont().deriveFont(Font.BOLD, 16f));
            g2d.drawString("Debut Ready!", getWidth() / 2 - 60, getHeight() / 2 - 40);
            g2d.setFont(getFont().deriveFont(13f));
            g2d.drawString("Press D to confirm debut", getWidth() / 2 - 90, getHeight() / 2 - 10);
        }
    }

    private void paintSurvivalOverlay(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, getWidth(), getHeight());
        g2d.setColor(Color.WHITE);
        g2d.fillRoundRect(80, 80, getWidth() - 160, getHeight() - 160, 20, 20);
        g2d.setColor(new Color(60, 60, 80));
        g2d.setFont(getFont().deriveFont(Font.BOLD, 18f));
        g2d.drawString("Survival Show: " + state.survivalRound.label, 120, 120);
        g2d.setFont(getFont().deriveFont(14f));
        g2d.drawString("Drag trainees onto Survival Show to enroll. Press Enter to resolve round.", 120, 150);

        int y = 190;
        for (TraineeAvatar avatar : state.avatars) {
            g2d.drawString(avatar.trainee.name + " ⭐" + avatar.trainee.starRating + " Pop " + avatar.trainee.popularity, 120, y);
            y += 24;
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Point p = e.getPoint();
        for (TraineeAvatar avatar : state.avatars) {
            if (avatar.getBounds().contains(p)) {
                dragging = avatar;
                dragOffset = new Point(p.x - avatar.x, p.y - avatar.y);
                break;
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (dragging != null) {
            Point p = e.getPoint();
            for (Room room : rooms) {
                if (room.rect.contains(p)) {
                    state.assignActivity(dragging, room.type, rng);
                }
            }
        }
        dragging = null;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (dragging != null) {
            Point p = e.getPoint();
            dragging.x = p.x - dragOffset.x;
            dragging.y = p.y - dragOffset.y;
        }
    }

    @Override public void mouseMoved(MouseEvent e) {}
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
}

enum ActivityType {
    DANCE("Dance practice"),
    VOCAL("Vocal training"),
    RAP("Rap training"),
    GYM("Stamina + Strength"),
    RECORDING("Recording session"),
    MEDIA("Variety / CF / Live"),
    REST("Rest & recovery"),
    SPECIAL("Missions & boosts"),
    EVALUATION("Monthly evaluation"),
    SURVIVAL("Survival show"),
    DEBUT("Debut prep");

    public final String description;
    ActivityType(String description) { this.description = description; }
}

enum SurvivalRound {
    AUDITION("Audition"),
    GROUP_BATTLE("Group Battle"),
    CONCEPT("Concept Battle"),
    FINAL("Final Stage");
    public final String label;
    SurvivalRound(String label) { this.label = label; }
    public SurvivalRound next() {
        switch (this) {
            case AUDITION: return GROUP_BATTLE;
            case GROUP_BATTLE: return CONCEPT;
            case CONCEPT: return FINAL;
            default: return FINAL;
        }
    }
}

class Room {
    final String name;
    final ActivityType type;
    final Rectangle rect;
    final Color color;

    Room(String name, ActivityType type, Rectangle rect, Color color) {
        this.name = name;
        this.type = type;
        this.rect = rect;
        this.color = color;
    }
}

class TraineeData {
    String name;
    int starRating; // 1-5
    int popularity; // 1-1000
    int vocal, dance, rap, visual, charisma, stamina;
    boolean debutEligible = false;

    TraineeData(String name, int stars, Random rng) {
        this.name = name;
        this.starRating = stars;
        this.popularity = rng.nextInt(50) + 1;
        this.vocal = 30 + rng.nextInt(20) + stars * 10;
        this.dance = 30 + rng.nextInt(20) + stars * 10;
        this.rap = 30 + rng.nextInt(20) + stars * 10;
        this.visual = 30 + rng.nextInt(20) + stars * 8;
        this.charisma = 30 + rng.nextInt(20) + stars * 8;
        this.stamina = 60 + rng.nextInt(30);
    }

    void applyActivity(ActivityType type, Random rng) {
        switch (type) {
            case DANCE: dance += 3 + starRating; stamina -= 4; break;
            case VOCAL: vocal += 3 + starRating; stamina -= 4; break;
            case RAP: rap += 3 + starRating; stamina -= 4; break;
            case GYM: stamina = Math.min(120, stamina + 6 + starRating); break;
            case RECORDING: popularity += 5 + rng.nextInt(8); stamina -= 6; break;
            case MEDIA: popularity += 8 + rng.nextInt(10); charisma += 2; stamina -= 5; break;
            case REST: stamina = Math.min(120, stamina + 10 + rng.nextInt(10)); break;
            case SPECIAL: applySpecial(rng); break;
            case EVALUATION: evaluate(rng); break;
            case SURVIVAL: survivalBoost(rng); break;
            case DEBUT: debutEligible = true; popularity += 20; break;
        }
        stamina = Math.max(10, stamina);
    }

    private void applySpecial(Random rng) {
        int roll = rng.nextInt(3);
        if (roll == 0) { popularity += 20; }
        else if (roll == 1) { stamina += 15; }
        else { charisma += 6; }
    }

    private void evaluate(Random rng) {
        int score = vocal + dance + rap + charisma + visual + rng.nextInt(40);
        if (score > 350) {
            popularity += 30;
            debutEligible = true;
        } else if (score > 250) {
            popularity += 10;
        } else {
            popularity = Math.max(1, popularity - 5);
        }
    }

    private void survivalBoost(Random rng) {
        popularity += 40 + starRating * 5 + rng.nextInt(30);
        charisma += 5;
        stamina -= 6;
    }
}

class TraineeAvatar {
    final TraineeData trainee;
    int x;
    int y;
    ActivityType current = ActivityType.REST;
    float anim = 0;

    TraineeAvatar(TraineeData data, int x, int y) {
        this.trainee = data;
        this.x = x;
        this.y = y;
    }

    Rectangle getBounds() {
        return new Rectangle(x, y, 32, 48);
    }

    void update(float delta) {
        anim += delta;
    }

    void paint(Graphics2D g2d) {
        int body = (int) (Math.abs(Math.sin(anim * 3)) * 2);
        Color outfit = switch (trainee.starRating) {
            case 5 -> new Color(255, 120, 180);
            case 4 -> new Color(120, 220, 255);
            case 3 -> new Color(180, 255, 140);
            case 2 -> new Color(255, 220, 120);
            default -> new Color(200, 200, 220);
        };
        g2d.setColor(new Color(60, 60, 80, 140));
        g2d.fill(new RoundRectangle2D.Float(x, y + 40, 32, 8, 8, 8));
        g2d.setColor(outfit);
        g2d.fillRect(x, y + body, 32, 32);
        g2d.setColor(new Color(255, 230, 200));
        g2d.fillRect(x + 8, y - 4 + body, 16, 16);
        g2d.setColor(Color.BLACK);
        g2d.fillRect(x + 12, y + 2 + body, 3, 3);
        g2d.fillRect(x + 18, y + 2 + body, 3, 3);
        g2d.setColor(Color.PINK);
        g2d.fillRect(x + 14, y + 8 + body, 4, 2);

        // floating activity icon
        g2d.setColor(new Color(0, 0, 0, 60));
        g2d.fillRoundRect(x - 4, y - 24, 40, 18, 8, 8);
        g2d.setColor(Color.WHITE);
        g2d.setFont(g2d.getFont().deriveFont(Font.PLAIN, 10f));
        g2d.drawString(current.name().toLowerCase(), x, y - 12);
    }

    String currentLabel() {
        return current.name();
    }
}

class GameState {
    List<TraineeAvatar> avatars = new ArrayList<>();
    int week = 1;
    int day = 1;
    int fans = 200;
    int reputation = 10;
    int companyMoney = 1000;
    Mode mode = Mode.CEO;
    SurvivalRound survivalRound = SurvivalRound.AUDITION;
    int debutReadyPopup = 0;
    private float dayTimer = 0f;

    GameState() {
        Random rng = new Random();
        avatars.add(new TraineeAvatar(new TraineeData("Yuna", 4, rng), 80, 520));
        avatars.add(new TraineeAvatar(new TraineeData("Minsu", 3, rng), 180, 520));
        avatars.add(new TraineeAvatar(new TraineeData("Lia", 5, rng), 280, 520));
    }

    void toggleMode() {
        mode = (mode == Mode.CEO) ? Mode.TRAINEE : Mode.CEO;
    }

    void update(float delta, List<Room> rooms, ActivityType focus, Random rng) {
        dayTimer += delta;
        if (dayTimer >= 4f) { // every few seconds a day passes
            dayTimer = 0f;
            day++;
            if (day > 7) {
                day = 1;
                week++;
                reputation += rng.nextInt(3);
                companyMoney += 100 + rng.nextInt(150);
            }
        }

        for (TraineeAvatar avatar : avatars) {
            avatar.update(delta);
            if (focus == ActivityType.SURVIVAL && avatar.current == ActivityType.SURVIVAL) {
                if (rng.nextFloat() < 0.005f) {
                    resolveSurvival(avatar, rng);
                }
            }
            if (rng.nextFloat() < 0.003f) {
                applyWeeklyEffect(avatar, rng);
            }
        }

        if (mode == Mode.TRAINEE) {
            TraineeAvatar main = avatars.get(0);
            if (main.trainee.debutEligible) {
                debutReadyPopup = 120; // frames
            }
        }
        if (debutReadyPopup > 0) {
            debutReadyPopup--;
        }
    }

    void assignActivity(TraineeAvatar avatar, ActivityType type, Random rng) {
        avatar.current = type;
        avatar.trainee.applyActivity(type, rng);
        if (type == ActivityType.DEBUT && avatar.trainee.debutEligible) {
            debut(avatar);
        }
        if (type == ActivityType.SURVIVAL) {
            // enrollment feedback handled in overlay
        }
    }

    private void applyWeeklyEffect(TraineeAvatar avatar, Random rng) {
        switch (avatar.current) {
            case DANCE -> avatar.trainee.dance += rng.nextInt(2);
            case VOCAL -> avatar.trainee.vocal += rng.nextInt(2);
            case RAP -> avatar.trainee.rap += rng.nextInt(2);
            case GYM -> avatar.trainee.stamina += rng.nextInt(3);
            case RECORDING -> avatar.trainee.popularity += rng.nextInt(5);
            case MEDIA -> avatar.trainee.popularity += rng.nextInt(4);
            case SPECIAL -> avatar.trainee.charisma += rng.nextInt(3);
            case EVALUATION -> {
                if (avatar.trainee.debutEligible) {
                    reputation += 5;
                }
            }
            case SURVIVAL -> {
                if (rng.nextFloat() < 0.002f) resolveSurvival(avatar, rng);
            }
            case REST -> avatar.trainee.stamina += rng.nextInt(4);
            case DEBUT -> {}
        }
    }

    private void resolveSurvival(TraineeAvatar avatar, Random rng) {
        int performance = avatar.trainee.starRating * 40 + avatar.trainee.popularity + rng.nextInt(120);
        if (performance > 180) {
            avatar.trainee.popularity += 80;
            avatar.trainee.debutEligible = true;
            reputation += 8;
            fans += 500;
            survivalRound = survivalRound.next();
        } else if (performance > 120) {
            avatar.trainee.popularity += 30;
            fans += 150;
        } else {
            avatar.trainee.popularity = Math.max(1, avatar.trainee.popularity - 20);
        }
    }

    private void debut(TraineeAvatar avatar) {
        fans += 1000;
        reputation += 20;
        companyMoney += 2000;
        avatar.trainee.popularity += 300;
        avatar.trainee.debutEligible = false;
    }

    void save() {
        List<String> lines = new ArrayList<>();
        lines.add(mode.name());
        lines.add(week + "," + day + "," + fans + "," + reputation + "," + companyMoney);
        for (TraineeAvatar avatar : avatars) {
            lines.add(String.join(",",
                    avatar.trainee.name,
                    String.valueOf(avatar.trainee.starRating),
                    String.valueOf(avatar.trainee.popularity),
                    String.valueOf(avatar.trainee.vocal),
                    String.valueOf(avatar.trainee.dance),
                    String.valueOf(avatar.trainee.rap),
                    String.valueOf(avatar.trainee.visual),
                    String.valueOf(avatar.trainee.charisma),
                    String.valueOf(avatar.trainee.stamina),
                    String.valueOf(avatar.trainee.debutEligible)
            ));
        }
        try {
            Files.write(Paths.get("idolab.save"), lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void load() {
        Path save = Paths.get("idolab.save");
        if (!Files.exists(save)) return;
        try {
            List<String> lines = Files.readAllLines(save);
            int idx = 0;
            mode = Mode.valueOf(lines.get(idx++));
            String[] meta = lines.get(idx++).split(",");
            week = Integer.parseInt(meta[0]);
            day = Integer.parseInt(meta[1]);
            fans = Integer.parseInt(meta[2]);
            reputation = Integer.parseInt(meta[3]);
            companyMoney = Integer.parseInt(meta[4]);
            avatars.clear();
            Random rng = new Random();
            while (idx < lines.size()) {
                String[] t = lines.get(idx++).split(",");
                TraineeData data = new TraineeData(t[0], Integer.parseInt(t[1]), rng);
                data.popularity = Integer.parseInt(t[2]);
                data.vocal = Integer.parseInt(t[3]);
                data.dance = Integer.parseInt(t[4]);
                data.rap = Integer.parseInt(t[5]);
                data.visual = Integer.parseInt(t[6]);
                data.charisma = Integer.parseInt(t[7]);
                data.stamina = Integer.parseInt(t[8]);
                data.debutEligible = Boolean.parseBoolean(t[9]);
                avatars.add(new TraineeAvatar(data, 80 + avatars.size() * 100, 520));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

enum Mode {
    CEO("CEO"),
    TRAINEE("Trainee");
    public final String label;
    Mode(String label) { this.label = label; }
}