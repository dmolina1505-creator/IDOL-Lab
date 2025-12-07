"""
IdoLab - Text-based prototype

Architecture overview (high-level outline before implementation):

Classes / Data Models
---------------------
- Player:
    - Fields: name, role ("CEO" or "Trainee"), character (Trainee instance when in trainee mode)
- Trainee:
    - Fields: name, vocal, dance, rap, visual, charisma, stamina, popularity, relationship, trainee_since
    - Methods: train(stat, amount), rest(), adjust_popularity(amount), adjust_relationship(amount)
- Company:
    - Fields: name, money, reputation, trainees (list[Trainee]), groups (list[Group]), projects (list[SongProject])
    - Methods: recruit_trainee(), schedule_training(), plan_debut(), release_song(), promote(), update_finances(amount)
- Group:
    - Fields: name, members (list[Trainee]), concept, popularity
    - Methods: calculate_power(), debut_effect(budget)
- SongProject:
    - Fields: title, budget, concept, success_roll
    - Methods: outcome(group_power)

Game Systems / Flow
-------------------
- GameState:
    - Fields: day (1..30), month (1..12), year, player, company, random_events_log, trainee_failures
    - Methods: advance_day(), monthly_evaluation(), check_victory_or_defeat()
- UI Helpers:
    - render_status(): prints date and key stats
    - prompt_action(): shows available actions depending on role
    - apply_action(): executes the chosen action and updates state

Main Loop
---------
- greet player and choose role (CEO or Trainee)
- initialize default company, trainees, and player character
- while game not over:
    - render_status()
    - show menu and read input
    - execute chosen action
    - advance to next day, handle random events, handle monthly evaluations
- display win/lose message

The code below implements this outline with clear functions that can be reused
in future graphical versions.
"""

from __future__ import annotations

import random
import sys
from dataclasses import dataclass, field
from typing import List, Optional


# ---------------------------
# Data Models
# ---------------------------


@dataclass
class Trainee:
    name: str
    vocal: int = 30
    dance: int = 30
    rap: int = 30
    visual: int = 30
    charisma: int = 30
    stamina: int = 70
    popularity: int = 10
    relationship: int = 10
    trainee_since: int = 1  # month index for flavor

    def train(self, stat: str, intensity: int = 5) -> str:
        """Increase a stat and reduce stamina, returning a log message."""
        base_gain = random.randint(intensity - 2, intensity + 2)
        self.stamina = max(0, self.stamina - random.randint(8, 12))
        message = ""
        if stat == "vocal":
            self.vocal += base_gain
            message = f"Vocal training +{base_gain}."
        elif stat == "dance":
            self.dance += base_gain
            message = f"Dance training +{base_gain}."
        elif stat == "rap":
            self.rap += base_gain
            message = f"Rap training +{base_gain}."
        elif stat == "charisma":
            self.charisma += base_gain
            message = f"Charisma workshop +{base_gain}."
        elif stat == "visual":
            self.visual += base_gain
            message = f"Visual coaching +{base_gain}."
        else:
            message = "Training focused on fundamentals."
        return message + " Stamina -10 (approx)."

    def rest(self) -> str:
        recovery = random.randint(10, 20)
        self.stamina = min(100, self.stamina + recovery)
        return f"Rested and recovered {recovery} stamina."

    def adjust_popularity(self, amount: int) -> None:
        self.popularity = max(0, self.popularity + amount)

    def adjust_relationship(self, amount: int) -> None:
        self.relationship = max(0, min(100, self.relationship + amount))

    def score(self) -> int:
        return self.vocal + self.dance + self.rap + self.visual + self.charisma


@dataclass
class Group:
    name: str
    members: List[Trainee]
    concept: str = "Fresh"
    popularity: int = 0

    def calculate_power(self) -> int:
        member_power = sum(m.score() for m in self.members) // max(len(self.members), 1)
        chemistry_bonus = random.randint(0, 15)
        return member_power + chemistry_bonus

    def debut_effect(self, budget: int) -> int:
        power = self.calculate_power()
        debut_pop = power // 5 + budget // 10 + random.randint(-10, 15)
        self.popularity += debut_pop
        for m in self.members:
            m.adjust_popularity(debut_pop // 2)
        return debut_pop


@dataclass
class SongProject:
    title: str
    budget: int
    concept: str = "Bright"

    def outcome(self, group_power: int) -> int:
        roll = random.randint(-15, 25)
        critical = random.random() < 0.1
        if critical:
            roll += 30
        success = group_power // 3 + self.budget // 15 + roll
        return success


@dataclass
class Company:
    name: str
    money: int = 5000
    reputation: int = 10
    trainees: List[Trainee] = field(default_factory=list)
    groups: List[Group] = field(default_factory=list)
    projects: List[SongProject] = field(default_factory=list)

    def recruit_trainee(self, name: str) -> Trainee:
        new = Trainee(name=name, vocal=random.randint(20, 40), dance=random.randint(20, 40), rap=random.randint(20, 40))
        self.trainees.append(new)
        self.money -= 300
        return new

    def schedule_training(self, trainee: Trainee, focus: str) -> str:
        cost = 150
        if self.money < cost:
            return "Not enough money for training."
        self.money -= cost
        message = trainee.train(focus)
        return f"Training scheduled for {trainee.name}. {message} Company money -{cost}."

    def plan_debut(self, names: List[str], concept: str, budget: int) -> str:
        if self.money < budget:
            return "Not enough money to plan debut."
        selected = [t for t in self.trainees if t.name in names]
        if not selected:
            return "No valid trainees selected."
        group = Group(name=f"Project {concept}", members=selected, concept=concept)
        self.groups.append(group)
        self.money -= budget
        pop_gain = group.debut_effect(budget)
        self.reputation += pop_gain // 10
        return f"Debuted group {group.name}! Popularity +{pop_gain}. Budget -{budget}."

    def release_song(self, group: Group, title: str, budget: int) -> str:
        if self.money < budget:
            return "Not enough funds for release."
        project = SongProject(title=title, budget=budget)
        self.projects.append(project)
        self.money -= budget
        power = group.calculate_power()
        result = project.outcome(power)
        group.popularity += result
        self.reputation += max(0, result // 8)
        return f"Released '{title}'. Result +{result} popularity for {group.name}. Cost {budget}."

    def promote(self, group: Group, budget: int) -> str:
        if self.money < budget:
            return "Not enough money for promotion."
        self.money -= budget
        gain = budget // 20 + random.randint(0, 10)
        group.popularity += gain
        self.reputation += gain // 3
        return f"Promotions boost {group.name}'s popularity by {gain}. Cost {budget}."

    def update_finances(self, amount: int) -> None:
        self.money += amount


@dataclass
class Player:
    name: str
    role: str
    character: Optional[Trainee] = None


@dataclass
class GameState:
    day: int = 1
    month: int = 1
    year: int = 1
    player: Optional[Player] = None
    company: Optional[Company] = None
    trainee_failures: int = 0
    message_log: List[str] = field(default_factory=list)

    def advance_day(self) -> None:
        self.day += 1
        if self.day > 30:
            self.day = 1
            self.month += 1
            if self.month > 12:
                self.month = 1
                self.year += 1
            self.message_log.append(self.monthly_evaluation())

    def monthly_evaluation(self) -> str:
        if self.player and self.player.role == "Trainee" and self.player.character:
            trainee = self.player.character
            score = trainee.score() + trainee.popularity
            threshold = 220 + self.year * 10
            if score >= threshold:
                trainee.adjust_relationship(5)
                trainee.adjust_popularity(5)
                return f"Monthly evaluation passed! Score {score} / {threshold}. Relationship +5."
            self.trainee_failures += 1
            penalty = random.randint(5, 10)
            trainee.adjust_relationship(-penalty)
            return f"Evaluation failed. Score {score} / {threshold}. Relationship -{penalty}."
        # CEO mode: small finance tick
        if self.company:
            income = sum(g.popularity // 2 for g in self.company.groups)
            self.company.update_finances(income)
            return f"Monthly income report: earned {income} from active groups."
        return "The month rolls by without major events."

    def check_victory_or_defeat(self) -> Optional[str]:
        # CEO conditions
        if self.player and self.player.role == "CEO" and self.company:
            if self.company.money < -2000:
                return "Bankruptcy! Your company could not pay its bills."
            if any(group.popularity >= 300 for group in self.company.groups):
                return "Success! One of your groups became a top act."
            if self.year > 5:
                return "Time's up. The industry moved on before you produced a hit." if self.company.reputation < 80 else "Legendary CEO!"
        # Trainee conditions
        if self.player and self.player.role == "Trainee" and self.player.character:
            trainee = self.player.character
            if trainee.popularity >= 200 and trainee.score() >= 350:
                return "You debuted and became a sensation!"
            if self.trainee_failures >= 3:
                return "You were cut after repeated failed evaluations."
            if self.year > 5:
                return "The window closed before you could debut." if trainee.popularity < 150 else "Late bloomer debut success!"
        return None


# ---------------------------
# Helper functions
# ---------------------------


def safe_int_input(prompt: str, min_value: int, max_value: int) -> int:
    while True:
        try:
            value = int(input(prompt))
        except ValueError:
            print("Please enter a number.")
            continue
        if min_value <= value <= max_value:
            return value
        print(f"Enter a value between {min_value} and {max_value}.")


def choose_role() -> str:
    print("Choose your path:")
    print("1) CEO - Run the company")
    print("2) Trainee - Fight to debut")
    choice = safe_int_input("Select 1 or 2: ", 1, 2)
    return "CEO" if choice == 1 else "Trainee"


def initialize_game() -> GameState:
    name = input("Enter your name (or stage name): ") or "Player"
    role = choose_role()
    player = Player(name=name, role=role)
    company = Company(name="IdoLab Entertainment")

    # Seed with a few default trainees for CEO mode
    starter_names = ["Ara", "Min", "Jisu", "Luna", "Kai"]
    for label in random.sample(starter_names, 3):
        company.trainees.append(Trainee(name=label, popularity=random.randint(5, 20)))

    if role == "Trainee":
        trainee_character = Trainee(name=name, vocal=35, dance=35, rap=30, charisma=35, visual=35, stamina=80)
        player.character = trainee_character
        company.trainees.append(trainee_character)

    return GameState(player=player, company=company)


# ---------------------------
# CEO Mode Actions
# ---------------------------


def ceo_actions(state: GameState) -> List[str]:
    return [
        "Recruit trainee",
        "Schedule training",
        "Plan debut",
        "Release song/album",
        "Schedule promotion",
        "Check company finances",
        "Rest / advance day",
    ]


def handle_ceo_action(state: GameState, choice: int) -> str:
    company = state.company
    assert company is not None

    if choice == 1:
        name = input("Enter new trainee name: ") or f"Trainee{len(company.trainees)+1}"
        new = company.recruit_trainee(name)
        return f"Recruited {new.name}! Money now {company.money}."
    if choice == 2:
        if not company.trainees:
            return "No trainees available."
        for idx, trainee in enumerate(company.trainees, start=1):
            print(f"{idx}) {trainee.name} (Sta {trainee.stamina})")
        t_idx = safe_int_input("Choose trainee: ", 1, len(company.trainees)) - 1
        focus = input("Focus (vocal/dance/rap/visual/charisma): ").strip().lower() or "vocal"
        return company.schedule_training(company.trainees[t_idx], focus)
    if choice == 3:
        if len(company.trainees) < 1:
            return "You need at least one trainee to debut."
        selected_names = []
        print("Select trainees by number, separated by commas (e.g., 1,2):")
        for idx, trainee in enumerate(company.trainees, start=1):
            print(f"{idx}) {trainee.name}")
        picks = input("Your picks: ")
        try:
            indices = [int(p.strip()) - 1 for p in picks.split(",") if p.strip()]
        except ValueError:
            indices = []
        for idx in indices:
            if 0 <= idx < len(company.trainees):
                selected_names.append(company.trainees[idx].name)
        concept = input("Concept (Fresh/Dark/Retro): ") or "Fresh"
        budget = safe_int_input("Budget (100-2000): ", 100, 2000)
        return company.plan_debut(selected_names, concept, budget)
    if choice == 4:
        if not company.groups:
            return "No groups have debuted yet."
        for idx, group in enumerate(company.groups, start=1):
            print(f"{idx}) {group.name} (Pop {group.popularity})")
        g_idx = safe_int_input("Choose group: ", 1, len(company.groups)) - 1
        title = input("Song/Album title: ") or "Untitled"
        budget = safe_int_input("Production budget (100-3000): ", 100, 3000)
        return company.release_song(company.groups[g_idx], title, budget)
    if choice == 5:
        if not company.groups:
            return "No groups to promote."
        for idx, group in enumerate(company.groups, start=1):
            print(f"{idx}) {group.name} (Pop {group.popularity})")
        g_idx = safe_int_input("Choose group: ", 1, len(company.groups)) - 1
        budget = safe_int_input("Promotion budget (50-1500): ", 50, 1500)
        return company.promote(company.groups[g_idx], budget)
    if choice == 6:
        trainees_summary = ", ".join(f"{t.name} Sta:{t.stamina} Pop:{t.popularity}" for t in company.trainees)
        return f"Money: {company.money}, Reputation: {company.reputation}. Trainees: {trainees_summary}"
    return "Taking a breather to plan the next move."


# ---------------------------
# Trainee Mode Actions
# ---------------------------


def trainee_actions(state: GameState) -> List[str]:
    return [
        "Train a skill",
        "Rest to regain stamina",
        "Build relationships",
        "Participate in evaluation",
        "View personal stats",
        "Advance day",
    ]


def handle_trainee_action(state: GameState, choice: int) -> str:
    trainee = state.player.character if state.player else None
    if trainee is None:
        return "No trainee data loaded."

    if choice == 1:
        focus = input("Focus (vocal/dance/rap/visual/charisma): ").strip().lower() or "dance"
        return trainee.train(focus)
    if choice == 2:
        return trainee.rest()
    if choice == 3:
        gain = random.randint(3, 10)
        trainee.adjust_relationship(gain)
        trainee.adjust_popularity(gain // 2)
        return f"Shared practice room gossip. Relationship +{gain}, Popularity +{gain//2}."
    if choice == 4:
        score = trainee.score() + random.randint(-10, 25)
        threshold = 210 + state.year * 10
        if score >= threshold:
            trainee.adjust_popularity(10)
            trainee.adjust_relationship(5)
            return f"Evaluation success! Score {score} / {threshold}. Popularity +10."
        state.trainee_failures += 1
        trainee.adjust_relationship(-5)
        return f"Evaluation tough. Score {score} / {threshold}. Failure count {state.trainee_failures}."
    if choice == 5:
        return (
            f"Stats - Vocal:{trainee.vocal} Dance:{trainee.dance} Rap:{trainee.rap} Visual:{trainee.visual} "
            f"Charisma:{trainee.charisma} Stamina:{trainee.stamina} Popularity:{trainee.popularity} "
            f"Relationship:{trainee.relationship}"
        )
    return "Day passes while you reflect on your dreams."


# ---------------------------
# Random events
# ---------------------------


def random_event(state: GameState) -> Optional[str]:
    roll = random.random()
    if roll < 0.08:
        if state.player.role == "CEO" and state.company:
            loss = random.randint(200, 800)
            state.company.money -= loss
            return f"Unexpected venue cancellation. Lost {loss} in fees."
        trainee = state.player.character if state.player else None
        if trainee:
            injury = random.randint(5, 15)
            trainee.stamina = max(0, trainee.stamina - injury)
            return f"Minor injury during rehearsal. Stamina -{injury}."
    if roll > 0.92:
        if state.player.role == "CEO" and state.company and state.company.groups:
            bonus = random.randint(300, 900)
            state.company.money += bonus
            top_group = max(state.company.groups, key=lambda g: g.popularity)
            top_group.popularity += bonus // 10
            return f"Viral moment! {top_group.name} trend boosts funds by {bonus}."
        trainee = state.player.character if state.player else None
        if trainee:
            boost = random.randint(8, 18)
            trainee.adjust_popularity(boost)
            return f"Fan edit goes viral. Popularity +{boost}."
    return None


# ---------------------------
# Rendering helpers
# ---------------------------


def render_status(state: GameState) -> None:
    print("\n" + "-" * 50)
    print(f"Day {state.day}, Month {state.month}, Year {state.year}")
    if state.player:
        print(f"Player: {state.player.name} ({state.player.role})")
    if state.company:
        print(f"Company Money: {state.company.money} | Reputation: {state.company.reputation}")
    if state.player and state.player.role == "Trainee" and state.player.character:
        t = state.player.character
        print(
            f"You - Sta:{t.stamina} Vocal:{t.vocal} Dance:{t.dance} Rap:{t.rap} Charisma:{t.charisma} "
            f"Pop:{t.popularity} Rel:{t.relationship}"
        )
    if state.player and state.player.role == "CEO" and state.company:
        trainee_info = ", ".join(f"{t.name} Pop:{t.popularity} Sta:{t.stamina}" for t in state.company.trainees)
        print(f"Trainees: {trainee_info if trainee_info else 'None'}")
        if state.company.groups:
            group_info = ", ".join(f"{g.name} Pop:{g.popularity}" for g in state.company.groups)
            print(f"Groups: {group_info}")

    if state.message_log:
        print("Recent events:")
        for msg in state.message_log[-3:]:
            print(f" * {msg}")
    print("-" * 50)


# ---------------------------
# Main game loop
# ---------------------------


def main() -> None:
    random.seed()
    print("Welcome to IdoLab: a K-pop management & trainee-life sim prototype!\n")
    state = initialize_game()

    while True:
        render_status(state)
        if state.player.role == "CEO":
            actions = ceo_actions(state)
        else:
            actions = trainee_actions(state)

        for idx, action in enumerate(actions, start=1):
            print(f"{idx}) {action}")
        choice = safe_int_input("Choose an action: ", 1, len(actions))

        if state.player.role == "CEO":
            result = handle_ceo_action(state, choice)
        else:
            result = handle_trainee_action(state, choice)
        state.message_log.append(result)
        print(result)

        event = random_event(state)
        if event:
            state.message_log.append(event)
            print(event)

        state.advance_day()
        verdict = state.check_victory_or_defeat()
        if verdict:
            print("\n" + "#" * 40)
            print(verdict)
            print("Thanks for playing the prototype!")
            break


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        sys.exit("\nExiting game. See you next time!")