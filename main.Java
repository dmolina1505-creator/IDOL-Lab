/**
 * IdoLab - Text-based prototype (Java version)
 *
 * Architecture overview:
 *
 * Classes / Data Models
 * ---------------------
 * - Player: name, role ("CEO" or "Trainee"), character (Trainee when in trainee mode)
 * - Trainee: stats for vocal/dance/rap/visual/charisma/stamina/popularity/relationship
 *     Methods: train(stat, intensity), rest(), adjustPopularity(amount), adjustRelationship(amount), score()
 * - Group: name, members, concept, popularity
 *     Methods: calculatePower(), debutEffect(budget)
 * - SongProject: title, budget, concept, successRoll()
 *     Methods: outcome(groupPower)
 * - Company: name, money, reputation, lists of trainees/groups/projects
 *     Methods: recruitTrainee(), scheduleTraining(), planDebut(), releaseSong(), promote(), updateFinances()
 * - GameState: day, month, year, player, company, failure counts, message log
 *     Methods: advanceDay(), monthlyEvaluation(), checkVictoryOrDefeat()
 *
 * Game Systems / Flow
 * -------------------
 * - Render status each turn with date, player info, finances, and recent events
 * - Present role-specific menus (CEO or Trainee) and resolve the chosen action
 * - Apply random events, advance the calendar, and check win/lose conditions
 * - The code is organized into small helpers to make future UI changes easier
 */

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Scanner;
import java.util.StringJoiner;

class Trainee {
    final String name;
    int vocal = 30;
    int dance = 30;
    int rap = 30;
    int visual = 30;
    int charisma = 30;
    int stamina = 70;
    int popularity = 10;
    int relationship = 10;
    int traineeSince = 1;

    Trainee(String name) {
        this.name = name;
    }

    Trainee(String name, int vocal, int dance, int rap, int visual, int charisma, int stamina, int popularity) {
        this.name = name;
        this.vocal = vocal;
        this.dance = dance;
        this.rap = rap;
        this.visual = visual;
        this.charisma = charisma;
        this.stamina = stamina;
        this.popularity = popularity;
    }

    String train(String stat, int intensity, Random rng) {
        int baseGain = randomBetween(rng, intensity - 2, intensity + 2);
        stamina = Math.max(0, stamina - randomBetween(rng, 8, 12));
        String message;
        switch (stat) {
            case "vocal":
                vocal += baseGain;
                message = "Vocal training +" + baseGain + ".";
                break;
            case "dance":
                dance += baseGain;
                message = "Dance training +" + baseGain + ".";
                break;
            case "rap":
                rap += baseGain;
                message = "Rap training +" + baseGain + ".";
                break;
            case "charisma":
                charisma += baseGain;
                message = "Charisma workshop +" + baseGain + ".";
                break;
            case "visual":
                visual += baseGain;
                message = "Visual coaching +" + baseGain + ".";
                break;
            default:
                message = "Training focused on fundamentals.";
        }
        return message + " Stamina -10 (approx).";
    }

    String rest(Random rng) {
        int recovery = randomBetween(rng, 10, 20);
        stamina = Math.min(100, stamina + recovery);
        return "Rested and recovered " + recovery + " stamina.";
    }

    void adjustPopularity(int amount) {
        popularity = Math.max(0, popularity + amount);
    }

    void adjustRelationship(int amount) {
        relationship = Math.max(0, Math.min(100, relationship + amount));
    }

    int score() {
        return vocal + dance + rap + visual + charisma;
    }

    private int randomBetween(Random rng, int min, int max) {
        return min + rng.nextInt(Math.max(1, (max - min) + 1));
    }
}

class Group {
    final String name;
    final List<Trainee> members;
    String concept = "Fresh";
    int popularity = 0;

    Group(String name, List<Trainee> members, String concept) {
        this.name = name;
        this.members = new ArrayList<>(members);
        this.concept = concept;
    }

    int calculatePower(Random rng) {
        int total = members.stream().mapToInt(Trainee::score).sum();
        int memberPower = members.isEmpty() ? 0 : total / members.size();
        int chemistryBonus = randomBetween(rng, 0, 15);
        return memberPower + chemistryBonus;
    }

    int debutEffect(int budget, Random rng) {
        int power = calculatePower(rng);
        int debutPop = power / 5 + budget / 10 + randomBetween(rng, -10, 15);
        popularity += debutPop;
        for (Trainee m : members) {
            m.adjustPopularity(debutPop / 2);
        }
        return debutPop;
    }

    private int randomBetween(Random rng, int min, int max) {
        return min + rng.nextInt(Math.max(1, (max - min) + 1));
    }
}

class SongProject {
    final String title;
    final int budget;
    String concept = "Bright";

    SongProject(String title, int budget) {
        this.title = title;
        this.budget = budget;
    }

    int outcome(int groupPower, Random rng) {
        int roll = randomBetween(rng, -15, 25);
        boolean critical = rng.nextDouble() < 0.1;
        if (critical) {
            roll += 30;
        }
        return groupPower / 3 + budget / 15 + roll;
    }

    private int randomBetween(Random rng, int min, int max) {
        return min + rng.nextInt(Math.max(1, (max - min) + 1));
    }
}

class Company {
    final String name;
    int money = 5000;
    int reputation = 10;
    final List<Trainee> trainees = new ArrayList<>();
    final List<Group> groups = new ArrayList<>();
    final List<SongProject> projects = new ArrayList<>();

    Company(String name) {
        this.name = name;
    }

    Trainee recruitTrainee(String name, Random rng) {
        Trainee trainee = new Trainee(
                name,
                randomBetween(rng, 20, 40),
                randomBetween(rng, 20, 40),
                randomBetween(rng, 20, 40),
                30,
                30,
                70,
                10
        );
        trainees.add(trainee);
        money -= 300;
        return trainee;
    }

    String scheduleTraining(Trainee trainee, String focus, Random rng) {
        int cost = 150;
        if (money < cost) {
            return "Not enough money for training.";
        }
        money -= cost;
        String message = trainee.train(focus, 5, rng);
        return "Training scheduled for " + trainee.name + ". " + message + " Company money -" + cost + ".";
    }

    String planDebut(List<String> names, String concept, int budget, Random rng) {
        if (money < budget) {
            return "Not enough money to plan debut.";
        }
        List<Trainee> selected = new ArrayList<>();
        for (String name : names) {
            trainees.stream().filter(t -> t.name.equalsIgnoreCase(name.trim())).findFirst().ifPresent(selected::add);
        }
        if (selected.isEmpty()) {
            return "No valid trainees selected.";
        }
        Group group = new Group("Project " + concept, selected, concept);
        groups.add(group);
        money -= budget;
        int popGain = group.debutEffect(budget, rng);
        reputation += popGain / 10;
        return "Debuted group " + group.name + "! Popularity +" + popGain + ". Budget -" + budget + ".";
    }

    String releaseSong(Group group, String title, int budget, Random rng) {
        if (money < budget) {
            return "Not enough funds for release.";
        }
        SongProject project = new SongProject(title, budget);
        projects.add(project);
        money -= budget;
        int power = group.calculatePower(rng);
        int result = project.outcome(power, rng);
        group.popularity += result;
        reputation += Math.max(0, result / 8);
        return "Released '" + title + "'. Result +" + result + " popularity for " + group.name + ". Cost " + budget + ".";
    }

    String promote(Group group, int budget, Random rng) {
        if (money < budget) {
            return "Not enough money for promotion.";
        }
        money -= budget;
        int gain = budget / 20 + randomBetween(rng, 0, 10);
        group.popularity += gain;
        reputation += gain / 3;
        return "Promotions boost " + group.name + "'s popularity by " + gain + ". Cost " + budget + ".";
    }

    void updateFinances(int amount) {
        money += amount;
    }

    private int randomBetween(Random rng, int min, int max) {
        return min + rng.nextInt(Math.max(1, (max - min) + 1));
    }
}

class Player {
    final String name;
    final String role;
    Trainee character;

    Player(String name, String role) {
        this.name = name;
        this.role = role;
    }
}

class GameState {
    int day = 1;
    int month = 1;
    int year = 1;
    Player player;
    Company company;
    int traineeFailures = 0;
    final List<String> messageLog = new ArrayList<>();
    final Random rng = new Random();

    GameState(Player player, Company company) {
        this.player = player;
        this.company = company;
    }

    void advanceDay() {
        day++;
        if (day > 30) {
            day = 1;
            month++;
            if (month > 12) {
                month = 1;
                year++;
            }
            messageLog.add(monthlyEvaluation());
        }
    }

    String monthlyEvaluation() {
        if (player != null && "Trainee".equals(player.role) && player.character != null) {
            Trainee trainee = player.character;
            int score = trainee.score() + trainee.popularity;
            int threshold = 220 + year * 10;
            if (score >= threshold) {
                trainee.adjustRelationship(5);
                trainee.adjustPopularity(5);
                return "Monthly evaluation passed! Score " + score + " / " + threshold + ". Relationship +5.";
            }
            traineeFailures++;
            int penalty = randomBetween(rng, 5, 10);
            trainee.adjustRelationship(-penalty);
            return "Evaluation failed. Score " + score + " / " + threshold + ". Relationship -" + penalty + ".";
        }
        if (company != null) {
            int income = company.groups.stream().mapToInt(g -> g.popularity / 2).sum();
            company.updateFinances(income);
            return "Monthly income report: earned " + income + " from active groups.";
        }
        return "The month rolls by without major events.";
    }

    Optional<String> checkVictoryOrDefeat() {
        if (player != null && "CEO".equals(player.role) && company != null) {
            if (company.money < -2000) {
                return Optional.of("Bankruptcy! Your company could not pay its bills.");
            }
            boolean hitGroup = company.groups.stream().anyMatch(g -> g.popularity >= 300);
            if (hitGroup) {
                return Optional.of("Success! One of your groups became a top act.");
            }
            if (year > 5) {
                return Optional.of(company.reputation < 80
                        ? "Time's up. The industry moved on before you produced a hit."
                        : "Legendary CEO!");
            }
        }
        if (player != null && "Trainee".equals(player.role) && player.character != null) {
            Trainee trainee = player.character;
            if (trainee.popularity >= 200 && trainee.score() >= 350) {
                return Optional.of("You debuted and became a sensation!");
            }
            if (traineeFailures >= 3) {
                return Optional.of("You were cut after repeated failed evaluations.");
            }
            if (year > 5) {
                return Optional.of(trainee.popularity < 150
                        ? "The window closed before you could debut."
                        : "Late bloomer debut success!");
            }
        }
        return Optional.empty();
    }

    private int randomBetween(Random rng, int min, int max) {
        return min + rng.nextInt(Math.max(1, (max - min) + 1));
    }
}

public class Main {
    private static final Scanner INPUT = new Scanner(System.in);

    public static void main(String[] args) {
        if (args.length > 0 && "--web".equalsIgnoreCase(args[0])) {
            GameState webState = initializeGame("Mobile CEO", "CEO");
            IdoLabHttpServer server = new IdoLabHttpServer(webState, 8080);
            server.start();
            return;
        }

        System.out.println("Welcome to IdoLab (Java edition): a K-pop management & trainee-life sim prototype!\n");
        GameState state = initializeGameFromPrompt();

        while (true) {
            renderStatus(state);
            List<String> actions = state.player.role.equals("CEO")
                    ? ceoActions()
                    : traineeActions();

            for (int i = 0; i < actions.size(); i++) {
                System.out.println((i + 1) + ") " + actions.get(i));
            }
            int choice = safeIntInput("Choose an action: ", 1, actions.size());

            String result = state.player.role.equals("CEO")
                    ? handleCeoAction(state, choice)
                    : handleTraineeAction(state, choice);
            state.messageLog.add(result);
            System.out.println(result);

            randomEvent(state).ifPresent(event -> {
                state.messageLog.add(event);
                System.out.println(event);
            });

            state.advanceDay();
            Optional<String> verdict = state.checkVictoryOrDefeat();
            if (verdict.isPresent()) {
                System.out.println("\n" + "#".repeat(40));
                System.out.println(verdict.get());
                System.out.println("Thanks for playing the prototype!");
                break;
            }
        }
    }

    private static GameState initializeGameFromPrompt() {
        System.out.print("Enter your name (or stage name): ");
        String name = readNonEmptyLine("Player");
        String role = chooseRole();
        return initializeGame(name, role);
    }

    private static GameState initializeGame(String name, String role) {
        Player player = new Player(name, role);
        Company company = new Company("IdoLab Entertainment");

        List<String> starterNames = List.of("Ara", "Min", "Jisu", "Luna", "Kai");
        List<String> shuffled = new ArrayList<>(starterNames);
        Collections.shuffle(shuffled);
        Random rng = new Random();
        for (int i = 0; i < 3; i++) {
            Trainee t = new Trainee(shuffled.get(i));
            t.popularity = randomBetween(rng, 5, 20);
            company.trainees.add(t);
        }

        if ("Trainee".equals(role)) {
            Trainee character = new Trainee(name, 35, 35, 30, 35, 35, 80, 10);
            player.character = character;
            company.trainees.add(character);
        }

        return new GameState(player, company);
    }

    private static String chooseRole() {
        System.out.println("Choose your path:");
        System.out.println("1) CEO - Run the company");
        System.out.println("2) Trainee - Fight to debut");
        int choice = safeIntInput("Select 1 or 2: ", 1, 2);
        return choice == 1 ? "CEO" : "Trainee";
    }

    private static List<String> ceoActions() {
        List<String> actions = new ArrayList<>();
        actions.add("Recruit trainee");
        actions.add("Schedule training");
        actions.add("Plan debut");
        actions.add("Release song/album");
        actions.add("Schedule promotion");
        actions.add("Check company finances");
        actions.add("Rest / advance day");
        return actions;
    }

    private static String handleCeoAction(GameState state, int choice) {
        Company company = state.company;
        Random rng = state.rng;
        switch (choice) {
            case 1:
                System.out.print("Enter new trainee name: ");
                String name = readNonEmptyLine("Trainee" + (company.trainees.size() + 1));
                Trainee newHire = company.recruitTrainee(name, rng);
                return "Recruited " + newHire.name + "! Money now " + company.money + ".";
            case 2:
                if (company.trainees.isEmpty()) {
                    return "No trainees available.";
                }
                showTrainees(company.trainees);
                int tIndex = safeIntInput("Choose trainee: ", 1, company.trainees.size()) - 1;
                System.out.print("Focus (vocal/dance/rap/visual/charisma): ");
                String focus = readNonEmptyLine("vocal").toLowerCase();
                return company.scheduleTraining(company.trainees.get(tIndex), focus, rng);
            case 3:
                if (company.trainees.isEmpty()) {
                    return "You need at least one trainee to debut.";
                }
                System.out.println("Select trainees by number, separated by commas (e.g., 1,2):");
                showTrainees(company.trainees);
                String picks = INPUT.nextLine();
                List<String> selectedNames = parseIndicesToNames(picks, company.trainees);
                System.out.print("Concept (Fresh/Dark/Retro): ");
                String concept = readNonEmptyLine("Fresh");
                int budget = safeIntInput("Budget (100-2000): ", 100, 2000);
                return company.planDebut(selectedNames, concept, budget, rng);
            case 4:
                if (company.groups.isEmpty()) {
                    return "No groups have debuted yet.";
                }
                showGroups(company.groups);
                int gIdx = safeIntInput("Choose group: ", 1, company.groups.size()) - 1;
                System.out.print("Song/Album title: ");
                String title = readNonEmptyLine("Untitled");
                int releaseBudget = safeIntInput("Production budget (100-3000): ", 100, 3000);
                return company.releaseSong(company.groups.get(gIdx), title, releaseBudget, rng);
            case 5:
                if (company.groups.isEmpty()) {
                    return "No groups to promote.";
                }
                showGroups(company.groups);
                int promoGroupIdx = safeIntInput("Choose group: ", 1, company.groups.size()) - 1;
                int promoBudget = safeIntInput("Promotion budget (50-1500): ", 50, 1500);
                return company.promote(company.groups.get(promoGroupIdx), promoBudget, rng);
            case 6:
                StringJoiner joiner = new StringJoiner(", ");
                for (Trainee t : company.trainees) {
                    joiner.add(t.name + " Sta:" + t.stamina + " Pop:" + t.popularity);
                }
                return "Money: " + company.money + ", Reputation: " + company.reputation + ". Trainees: "
                        + (joiner.length() > 0 ? joiner : "None");
            default:
                return "Taking a breather to plan the next move.";
        }
    }

    private static List<String> traineeActions() {
        List<String> actions = new ArrayList<>();
        actions.add("Train a skill");
        actions.add("Rest to regain stamina");
        actions.add("Build relationships");
        actions.add("Participate in evaluation");
        actions.add("View personal stats");
        actions.add("Advance day");
        return actions;
    }

    private static String handleTraineeAction(GameState state, int choice) {
        Trainee trainee = state.player.character;
        Random rng = state.rng;
        switch (choice) {
            case 1:
                System.out.print("Focus (vocal/dance/rap/visual/charisma): ");
                String focus = readNonEmptyLine("dance").toLowerCase();
                return trainee.train(focus, 5, rng);
            case 2:
                return trainee.rest(rng);
            case 3:
                int gain = randomBetween(rng, 3, 10);
                trainee.adjustRelationship(gain);
                trainee.adjustPopularity(gain / 2);
                return "Shared practice room gossip. Relationship +" + gain + ", Popularity +" + (gain / 2) + ".";
            case 4:
                int score = trainee.score() + randomBetween(rng, -10, 25);
                int threshold = 210 + state.year * 10;
                if (score >= threshold) {
                    trainee.adjustPopularity(10);
                    trainee.adjustRelationship(5);
                    return "Evaluation success! Score " + score + " / " + threshold + ". Popularity +10.";
                }
                state.traineeFailures++;
                trainee.adjustRelationship(-5);
                return "Evaluation tough. Score " + score + " / " + threshold + ". Failure count " + state.traineeFailures + ".";
            case 5:
                return "Stats - Vocal:" + trainee.vocal + " Dance:" + trainee.dance + " Rap:" + trainee.rap
                        + " Visual:" + trainee.visual + " Charisma:" + trainee.charisma + " Stamina:" + trainee.stamina
                        + " Popularity:" + trainee.popularity + " Relationship:" + trainee.relationship;
            default:
                return "Day passes while you reflect on your dreams.";
        }
    }

    private static Optional<String> randomEvent(GameState state) {
        double roll = state.rng.nextDouble();
        if (roll < 0.08) {
            if ("CEO".equals(state.player.role) && state.company != null) {
                int loss = randomBetween(state.rng, 200, 800);
                state.company.money -= loss;
                return Optional.of("Unexpected venue cancellation. Lost " + loss + " in fees.");
            }
            Trainee trainee = state.player.character;
            if (trainee != null) {
                int injury = randomBetween(state.rng, 5, 15);
                trainee.stamina = Math.max(0, trainee.stamina - injury);
                return Optional.of("Minor injury during rehearsal. Stamina -" + injury + ".");
            }
        }
        if (roll > 0.92) {
            if ("CEO".equals(state.player.role) && state.company != null && !state.company.groups.isEmpty()) {
                int bonus = randomBetween(state.rng, 300, 900);
                state.company.money += bonus;
                Group topGroup = state.company.groups.stream()
                        .max((g1, g2) -> Integer.compare(g1.popularity, g2.popularity))
                        .orElse(state.company.groups.get(0));
                topGroup.popularity += bonus / 10;
                return Optional.of("Viral moment! " + topGroup.name + " trend boosts funds by " + bonus + ".");
            }
            Trainee trainee = state.player.character;
            if (trainee != null) {
                int boost = randomBetween(state.rng, 8, 18);
                trainee.adjustPopularity(boost);
                return Optional.of("Fan edit goes viral. Popularity +" + boost + ".");
            }
        }
        return Optional.empty();
    }

    private static void renderStatus(GameState state) {
        System.out.println("\n" + "-".repeat(50));
        System.out.println("Day " + state.day + ", Month " + state.month + ", Year " + state.year);
        if (state.player != null) {
            System.out.println("Player: " + state.player.name + " (" + state.player.role + ")");
        }
        if (state.company != null) {
            System.out.println("Company Money: " + state.company.money + " | Reputation: " + state.company.reputation);
        }
        if (state.player != null && "Trainee".equals(state.player.role) && state.player.character != null) {
            Trainee t = state.player.character;
            System.out.println("You - Sta:" + t.stamina + " Vocal:" + t.vocal + " Dance:" + t.dance + " Rap:" + t.rap
                    + " Charisma:" + t.charisma + " Pop:" + t.popularity + " Rel:" + t.relationship);
        }
        if (state.player != null && "CEO".equals(state.player.role) && state.company != null) {
            StringJoiner traineeInfo = new StringJoiner(", ");
            for (Trainee t : state.company.trainees) {
                traineeInfo.add(t.name + " Pop:" + t.popularity + " Sta:" + t.stamina);
            }
            System.out.println("Trainees: " + (traineeInfo.length() > 0 ? traineeInfo : "None"));
            if (!state.company.groups.isEmpty()) {
                StringJoiner groupInfo = new StringJoiner(", ");
                for (Group g : state.company.groups) {
                    groupInfo.add(g.name + " Pop:" + g.popularity);
                }
                System.out.println("Groups: " + groupInfo);
            }
        }
        if (!state.messageLog.isEmpty()) {
            System.out.println("Recent events:");
            state.messageLog.stream().skip(Math.max(0, state.messageLog.size() - 3)).forEach(msg -> System.out.println(" * " + msg));
        }
        System.out.println("-".repeat(50));
    }

    private static int safeIntInput(String prompt, int minValue, int maxValue) {
        while (true) {
            System.out.print(prompt);
            String line = INPUT.nextLine();
            try {
                int value = Integer.parseInt(line.trim());
                if (value >= minValue && value <= maxValue) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // continue loop
            }
            System.out.println("Enter a value between " + minValue + " and " + maxValue + ".");
        }
    }

    private static String readNonEmptyLine(String fallback) {
        String line = INPUT.nextLine();
        if (line == null || line.trim().isEmpty()) {
            return fallback;
        }
        return line.trim();
    }

    private static int randomBetween(Random rng, int min, int max) {
        return min + rng.nextInt(Math.max(1, (max - min) + 1));
    }

    private static void showTrainees(List<Trainee> trainees) {
        for (int i = 0; i < trainees.size(); i++) {
            Trainee t = trainees.get(i);
            System.out.println((i + 1) + ") " + t.name + " (Sta " + t.stamina + ")");
        }
    }

    private static void showGroups(List<Group> groups) {
        for (int i = 0; i < groups.size(); i++) {
            Group g = groups.get(i);
            System.out.println((i + 1) + ") " + g.name + " (Pop " + g.popularity + ")");
        }
    }

    private static List<String> parseIndicesToNames(String picks, List<Trainee> trainees) {
        List<String> selected = new ArrayList<>();
        if (picks == null || picks.isBlank()) {
            return selected;
        }
        String[] tokens = picks.split(",");
        for (String token : tokens) {
            try {
                int idx = Integer.parseInt(token.trim()) - 1;
                if (idx >= 0 && idx < trainees.size()) {
                    selected.add(trainees.get(idx).name);
                }
            } catch (NumberFormatException ignored) {
                // skip invalid token
            }
        }
        return selected;
    }
}

class IdoLabHttpServer {
    private final GameState state;
    private final int port;

    IdoLabHttpServer(GameState state, int port) {
        this.state = state;
        this.port = port;
    }

    void start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", new RootHandler(state));
            server.createContext("/state", new StateHandler(state));
            server.createContext("/action", new ActionHandler(state));
            server.setExecutor(null);
            server.start();
            System.out.println("Web server running on http://localhost:" + port + " (try it from your phone or PC browser)\n"
                    + "Endpoints:\n"
                    + "- /state to view the current day/status\n"
                    + "- /action?type=... to perform actions (see README section in /)\n"
                    + "Press Ctrl+C to stop.");
        } catch (IOException e) {
            System.err.println("Failed to start web server: " + e.getMessage());
        }
    }

    private static class RootHandler implements HttpHandler {
        private final GameState state;

        RootHandler(GameState state) {
            this.state = state;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = "<html><body>"
                    + "<h1>IdoLab web mode</h1>"
                    + "<p>This lightweight server lets you test the game from a mobile or desktop browser.</p>"
                    + "<p>Quick links: <a href=\"/state\">View state</a></p>"
                    + "<h2>CEO actions</h2>"
                    + "<ul>"
                    + "<li>Recruit: /action?type=recruit&name=Nova</li>"
                    + "<li>Train trainee: /action?type=train&trainee=1&focus=vocal</li>"
                    + "<li>Plan debut: /action?type=debut&members=1,2&concept=Fresh&budget=500</li>"
                    + "<li>Release song: /action?type=release&group=1&title=Shine&budget=800</li>"
                    + "<li>Promote: /action?type=promote&group=1&budget=400</li>"
                    + "<li>Check finances: /action?type=finances</li>"
                    + "<li>Advance day: /action?type=advance</li>"
                    + "</ul>"
                    + "<h2>Trainee actions</h2>"
                    + "<ul>"
                    + "<li>Train skill: /action?role=Trainee&type=train&focus=dance</li>"
                    + "<li>Rest: /action?role=Trainee&type=rest</li>"
                    + "<li>Relationships: /action?role=Trainee&type=relationship</li>"
                    + "<li>Evaluation: /action?role=Trainee&type=evaluation</li>"
                    + "<li>View stats: /action?role=Trainee&type=stats</li>"
                    + "<li>Advance day: /action?role=Trainee&type=advance</li>"
                    + "</ul>"
                    + "<p>Tip: Switch the player role by adding <code>&role=Trainee</code> to the URL. CEO is the default.</p>"
                    + renderStateHtml(state)
                    + "</body></html>";
            respond(exchange, body);
        }
    }

    private static class StateHandler implements HttpHandler {
        private final GameState state;

        StateHandler(GameState state) {
            this.state = state;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = "<html><body>" + renderStateHtml(state) + "</body></html>";
            respond(exchange, body);
        }
    }

    private static class ActionHandler implements HttpHandler {
        private final GameState state;

        ActionHandler(GameState state) {
            this.state = state;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = parseQuery(exchange.getRequestURI());
            String roleParam = params.getOrDefault("role", state.player.role);
            String type = params.getOrDefault("type", "advance");
            String result;
            switch (roleParam) {
                case "Trainee":
                    result = traineeWebAction(state, type, params);
                    break;
                default:
                    result = ceoWebAction(state, type, params);
            }

            state.advanceDay();
            state.messageLog.add(result);
            StringBuilder body = new StringBuilder();
            body.append("<html><body>");
            body.append("<p><strong>Action:</strong> ").append(type).append("</p>");
            body.append("<p>").append(result).append("</p>");
            body.append(renderStateHtml(state));
            body.append("<p><a href=\"/\">Back to help</a></p>");
            body.append("</body></html>");
            respond(exchange, body.toString());
        }
    }

    private static void respond(HttpExchange exchange, String body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static Map<String, String> parseQuery(URI uri) {
        Map<String, String> params = new HashMap<>();
        String raw = uri.getRawQuery();
        if (raw == null || raw.isEmpty()) {
            return params;
        }
        for (String part : raw.split("&")) {
            String[] bits = part.split("=", 2);
            String key = URLDecoder.decode(bits[0], StandardCharsets.UTF_8);
            String value = bits.length > 1 ? URLDecoder.decode(bits[1], StandardCharsets.UTF_8) : "";
            params.put(key, value);
        }
        return params;
    }

    private static String renderStateHtml(GameState state) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h3>Day ").append(state.day).append(" / Month ").append(state.month).append(" / Year ").append(state.year).append("</h3>");
        sb.append("<p>Role: ").append(state.player.role).append(" | Money: ").append(state.company.money)
                .append(" | Reputation: ").append(state.company.reputation).append("</p>");
        sb.append("<h4>Trainees</h4><ul>");
        for (int i = 0; i < state.company.trainees.size(); i++) {
            Trainee t = state.company.trainees.get(i);
            sb.append("<li>").append(i + 1).append(") ").append(t.name)
                    .append(" Voc ").append(t.vocal)
                    .append(" Dan ").append(t.dance)
                    .append(" Rap ").append(t.rap)
                    .append(" Sta ").append(t.stamina)
                    .append(" Pop ").append(t.popularity).append("</li>");
        }
        sb.append("</ul>");
        sb.append("<h4>Groups</h4><ul>");
        for (int i = 0; i < state.company.groups.size(); i++) {
            Group g = state.company.groups.get(i);
            sb.append("<li>").append(i + 1).append(") ").append(g.name)
                    .append(" Pop ").append(g.popularity).append(" Concept ").append(g.concept).append("</li>");
        }
        sb.append("</ul>");
        if (state.player.character != null) {
            Trainee me = state.player.character;
            sb.append("<h4>Your trainee stats</h4><p>").append(me.vocal).append(" vocal, ")
                    .append(me.dance).append(" dance, ")
                    .append(me.rap).append(" rap, ")
                    .append(me.visual).append(" visual, ")
                    .append(me.charisma).append(" charisma, Stamina ")
                    .append(me.stamina).append(", Popularity ").append(me.popularity)
                    .append("</p>");
        }
        sb.append("<h4>Recent log</h4><ul>");
        int start = Math.max(0, state.messageLog.size() - 5);
        for (int i = start; i < state.messageLog.size(); i++) {
            sb.append("<li>").append(state.messageLog.get(i)).append("</li>");
        }
        sb.append("</ul>");
        return sb.toString();
    }

    private static String ceoWebAction(GameState state, String type, Map<String, String> params) {
        Company company = state.company;
        Random rng = state.rng;
        switch (type) {
            case "recruit":
                String name = params.getOrDefault("name", "NewFace" + (company.trainees.size() + 1));
                Trainee newHire = company.recruitTrainee(name, rng);
                return "Recruited " + newHire.name + ". Money now " + company.money + ".";
            case "train":
                int tIndex = parseIndex(params.get("trainee"), company.trainees.size());
                String focus = params.getOrDefault("focus", "vocal");
                return company.scheduleTraining(company.trainees.get(tIndex), focus, rng);
            case "debut":
                if (company.trainees.isEmpty()) {
                    return "Need at least one trainee to debut.";
                }
                String memberParam = params.getOrDefault("members", "1");
                List<String> selectedNames = parseIndicesToNames(memberParam, company.trainees);
                String concept = params.getOrDefault("concept", "Fresh");
                int budget = parseInt(params.getOrDefault("budget", "500"), 100, 3000);
                return company.planDebut(selectedNames, concept, budget, rng);
            case "release":
                if (company.groups.isEmpty()) {
                    return "No groups to release music.";
                }
                int gIndex = parseIndex(params.get("group"), company.groups.size());
                String title = params.getOrDefault("title", "Untitled");
                int releaseBudget = parseInt(params.getOrDefault("budget", "800"), 100, 4000);
                return company.releaseSong(company.groups.get(gIndex), title, releaseBudget, rng);
            case "promote":
                if (company.groups.isEmpty()) {
                    return "No groups to promote.";
                }
                int promoGroupIdx = parseIndex(params.get("group"), company.groups.size());
                int promoBudget = parseInt(params.getOrDefault("budget", "400"), 50, 2000);
                return company.promote(company.groups.get(promoGroupIdx), promoBudget, rng);
            case "finances":
                return "Money: " + company.money + ", Reputation: " + company.reputation + ".";
            default:
                return "Advanced a day to plan next moves.";
        }
    }

    private static String traineeWebAction(GameState state, String type, Map<String, String> params) {
        Trainee trainee = state.player.character;
        if (trainee == null) {
            return "No trainee character available.";
        }
        Random rng = state.rng;
        switch (type) {
            case "train":
                String focus = params.getOrDefault("focus", "vocal");
                return trainee.train(focus, 10, rng);
            case "rest":
                return trainee.rest(rng);
            case "relationship":
                int gain = randomBetween(rng, 3, 8);
                trainee.adjustRelationship(gain);
                return "Spent time bonding. Relationship +" + gain + ".";
            case "evaluation":
                return state.monthlyEvaluation();
            case "stats":
                return "Stats => Vocal " + trainee.vocal + ", Dance " + trainee.dance + ", Rap " + trainee.rap
                        + ", Charisma " + trainee.charisma + ", Visual " + trainee.visual + ", Popularity " + trainee.popularity
                        + ", Stamina " + trainee.stamina + ".";
            default:
                return "Advanced a day to recover.";
        }
    }

    private static int parseIndex(String raw, int size) {
        try {
            int idx = Integer.parseInt(raw) - 1;
            if (idx >= 0 && idx < size) {
                return idx;
            }
        } catch (NumberFormatException ignored) {
        }
        return 0;
    }

    private static int parseInt(String raw, int min, int max) {
        try {
            int value = Integer.parseInt(raw);
            if (value < min) {
                return min;
            }
            if (value > max) {
                return max;
            }
            return value;
        } catch (NumberFormatException e) {
            return min;
        }
    }

    private static List<String> parseIndicesToNames(String picks, List<Trainee> trainees) {
        List<String> selected = new ArrayList<>();
        if (picks == null || picks.isBlank()) {
            return selected;
        }
        String[] tokens = picks.split(",");
        for (String token : tokens) {
            try {
                int idx = Integer.parseInt(token.trim()) - 1;
                if (idx >= 0 && idx < trainees.size()) {
                    selected.add(trainees.get(idx).name);
                }
            } catch (NumberFormatException ignored) {
                // skip invalid token
            }
        }
        return selected;
    }

    private static int randomBetween(Random rng, int min, int max) {
        return min + rng.nextInt(Math.max(1, (max - min) + 1));
    }
}