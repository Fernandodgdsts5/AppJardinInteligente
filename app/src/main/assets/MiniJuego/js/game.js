const STORAGE_KEY = "jardin-inteligente-progreso";
const DESIGN_WIDTH = 1366;
const DESIGN_HEIGHT = 768;
const ROUND_COUNT = 6;
const LEVELS = [
    { name: "Semilla", minimum: 0, next: 100 },
    { name: "Brote", minimum: 100, next: 300 },
    { name: "Guardián verde", minimum: 300, next: 600 },
    { name: "Maestro del agua", minimum: 600, next: 1000 }
];

const SCENARIOS = [
    {
        id: "s1",
        plant: "geranio.png",
        plantName: "Geranio",
        plantAlt: "Geranio de flores rojas en una maceta",
        pet: "af.png",
        petName: "Miel",
        moisture: 22,
        temperature: 28,
        watered: "Hace 18 horas",
        answer: "REGAR",
        description: "El sustrato está seco y el día es cálido.",
        tip: "Con menos de 30% de humedad, una planta común necesita riego.",
        explanation: "Con 22% de humedad, este geranio necesita agua para recuperarse."
    },
    {
        id: "s2",
        plant: "helecho.png",
        plantName: "Helecho",
        plantAlt: "Helecho frondoso en una maceta clara",
        pet: "cf.png",
        petName: "Troll",
        moisture: 82,
        temperature: 21,
        watered: "Hace 1 hora",
        answer: "ESPERAR",
        description: "La tierra está muy húmeda y se regó hace poco.",
        tip: "El exceso de agua desplaza el aire que las raíces necesitan.",
        explanation: "Con 82% de humedad, conviene esperar y dejar que el sustrato drene."
    },
    {
        id: "s3",
        plant: "tomate.png",
        plantName: "Tomatera",
        plantAlt: "Planta de tomate con frutos rojos",
        pet: "gf.png",
        petName: "Coco",
        moisture: 34,
        temperature: 32,
        watered: "Hace 6 horas",
        answer: "REGAR",
        description: "Hace mucho calor y la tomatera pierde agua rápidamente.",
        tip: "El calor acelera la evaporación y el consumo de agua.",
        explanation: "Con calor intenso y solo 34% de humedad, la tomatera agradecerá un riego."
    },
    {
        id: "s4",
        plant: "geranio.png",
        plantName: "Geranio",
        plantAlt: "Geranio de flores rojas en una maceta",
        pet: "hf.png",
        petName: "Luna",
        moisture: 74,
        temperature: 19,
        watered: "Hace 2 horas",
        answer: "ESPERAR",
        description: "El ambiente está fresco y la humedad del sustrato es alta.",
        tip: "Si la tierra sigue húmeda, espera antes de volver a regar.",
        explanation: "Con 74% de humedad y clima fresco, otro riego sería innecesario."
    },
    {
        id: "s5",
        plant: "helecho.png",
        plantName: "Helecho joven",
        plantAlt: "Helecho frondoso en una maceta clara",
        pet: "lf.png",
        petName: "Gringo",
        moisture: 28,
        temperature: 25,
        watered: "Hace 12 horas",
        answer: "REGAR",
        description: "El helecho joven tiene poca humedad disponible en sus raíces.",
        tip: "Las plantas jóvenes tienen raíces pequeñas y necesitan atención frecuente.",
        explanation: "Con 28% de humedad, un riego moderado ayudará a este helecho joven."
    },
    {
        id: "s6",
        plant: "tomate.png",
        plantName: "Tomatera",
        plantAlt: "Planta de tomate con frutos rojos",
        pet: "rf.png",
        petName: "Oscar",
        moisture: 48,
        temperature: 23,
        watered: "Ayer",
        answer: "ESPERAR",
        description: "La humedad está en un nivel adecuado y la temperatura es templada.",
        tip: "No riegues solo por rutina: comprueba primero la humedad del suelo.",
        explanation: "Con 48% de humedad y temperatura templada, la tomatera puede esperar."
    }
];

const state = {
    phase: "start",
    roundIndex: 0,
    correctAnswers: 0,
    streak: 0,
    scenarios: [],
    rewards: { coins: 0, xp: 0 },
    sessionId: ""
};

const profile = { coins: 0, xp: 0 };
const elements = {};

function findElements() {
    const ids = [
        "app", "garden", "total-coins", "total-xp", "sensor-moisture",
        "sensor-temperature", "sensor-watered", "plant-health", "level-number",
        "level-name", "round-count", "level-progress", "xp-progress", "streak",
        "plant-image", "plant-label", "pet-bubble", "pet-image", "pet-name",
        "round-label", "scenario-copy", "tip-line", "feedback", "feedback-title",
        "feedback-copy", "continue-button", "action-list", "start-overlay",
        "result-overlay", "result-heading", "result-score", "result-message",
        "earned-coins", "earned-xp", "storage-notice"
    ];

    for (const id of ids) {
        elements[id] = document.getElementById(id);
    }
}

function showStorageNotice(msg) {
    if (!elements["storage-notice"]) return;
    if (window.MinijuegoHost && window.MinijuegoHost.isAndroid && !msg) {
        elements["storage-notice"].hidden = true;
        return;
    }
    elements["storage-notice"].textContent = msg ||
        "El progreso no se pudo guardar en este dispositivo; seguirá disponible mientras esta página esté abierta.";
    elements["storage-notice"].hidden = false;
}

function showDailyLimitNotice() {
    showStorageNotice("Límite diario de recompensas alcanzado (máximo 5.000 oro / 5.000 XP al día). ¡Sigue practicando!");
}

function loadProfile() {
    if (window.MinijuegoHost && window.MinijuegoHost.isAndroid) return; // Bypasses localStorage in Android mode

    try {
        const saved = localStorage.getItem(STORAGE_KEY);
        if (!saved) return;

        const data = JSON.parse(saved);
        if (
            !data ||
            !Number.isFinite(data.coins) ||
            !Number.isFinite(data.xp) ||
            data.coins < 0 ||
            data.xp < 0
        ) {
            throw new Error("El progreso guardado tiene un formato inválido.");
        }

        profile.coins = Math.floor(data.coins);
        profile.xp = Math.floor(data.xp);
    } catch (error) {
        showStorageNotice();
    }
}

function saveProfile() {
    if (window.MinijuegoHost && window.MinijuegoHost.isAndroid) return; // Bypasses localStorage in Android mode

    try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(profile));
    } catch (error) {
        showStorageNotice();
    }
}

function renderProfile() {
    if (!elements["total-coins"] || !elements["total-xp"]) return;

    elements["total-coins"].textContent = profile.coins.toLocaleString("es");
    elements["total-xp"].textContent = profile.xp.toLocaleString("es");

    let levelIndex = LEVELS.findIndex((level) => profile.xp < level.next);
    if (levelIndex === -1) levelIndex = LEVELS.length - 1;

    const level = LEVELS[levelIndex];
    const progress = Math.min(
        100,
        Math.max(0, ((profile.xp - level.minimum) / (level.next - level.minimum)) * 100)
    );

    if (elements["level-number"]) elements["level-number"].textContent = String(levelIndex + 1);
    if (elements["level-name"]) elements["level-name"].textContent = level.name;
    if (elements["xp-progress"]) {
        elements["xp-progress"].textContent = `${Math.min(profile.xp, level.next)} / ${level.next} XP`;
    }
    if (elements["level-progress"]) {
        elements["level-progress"].style.width = `${progress}%`;
        elements["level-progress"].parentElement.setAttribute("aria-valuenow", String(Math.round(progress)));
    }
}

function shuffleScenarios() {
    const scenarios = [...SCENARIOS];
    for (let index = scenarios.length - 1; index > 0; index -= 1) {
        const swapIndex = Math.floor(Math.random() * (index + 1));
        [scenarios[index], scenarios[swapIndex]] = [scenarios[swapIndex], scenarios[index]];
    }
    return scenarios;
}

function startGame() {
    state.phase = "playing";
    state.roundIndex = 0;
    state.correctAnswers = 0;
    state.streak = 0;
    state.rewards = { coins: 0, xp: 0 };
    state.sessionId = "session_" + Date.now() + "_" + Math.floor(Math.random() * 1000);
    state.scenarios = shuffleScenarios();

    elements["start-overlay"].hidden = true;
    elements["result-overlay"].hidden = true;
    elements["garden"].dataset.sky = "day";
    elements["feedback"].hidden = true;
    elements["tip-line"].hidden = false;
    renderQuestion();
}

function renderQuestion() {
    const scenario = state.scenarios[state.roundIndex];
    const progress = state.roundIndex + 1;

    state.phase = "playing";
    elements["round-count"].textContent = `Ronda ${progress} de ${ROUND_COUNT}`;
    elements["round-label"].textContent = `RONDA ${progress} DE ${ROUND_COUNT}`;
    elements["sensor-moisture"].textContent = `💧 ${scenario.moisture}%`;
    elements["sensor-temperature"].textContent = `🌡️ ${scenario.temperature}°C`;
    elements["sensor-watered"].textContent = `⏳ ${scenario.watered}`;
    elements["plant-label"].textContent = `${scenario.plantName} · Jardín inteligente`;
    elements["plant-image"].src = `imagenes/plantas/${scenario.plant}`;
    elements["plant-image"].alt = scenario.plantAlt;
    elements["pet-image"].src = `imagenes/mascotas/${scenario.pet}`;
    elements["pet-image"].alt = scenario.petName;
    elements["pet-name"].textContent = scenario.petName;
    elements["pet-bubble"].hidden = false;
    elements["scenario-copy"].textContent = scenario.description;
    elements["tip-line"].textContent = scenario.tip;
    elements["tip-line"].hidden = false;
    elements["feedback"].hidden = true;
    elements["streak"].textContent = `🔥 Racha: ${state.streak}`;
    elements["sensor-moisture"].dataset.state =
        scenario.moisture < 30 ? "dry" : scenario.moisture > 70 ? "wet" : "ok";
    elements["plant-health"].dataset.state =
        scenario.moisture < 30 ? "dry" : scenario.moisture > 70 ? "wet" : "ok";
    elements["plant-health"].textContent =
        scenario.moisture < 30 ? "🥀 Sedienta" : scenario.moisture > 70 ? "💧 Húmeda" : "👍 Saludable";

    for (const button of elements["action-list"].querySelectorAll("button")) {
        button.disabled = false;
        button.classList.remove("is-correct", "is-wrong");
    }
}

function submitAnswer(answer, button) {
    if (state.phase !== "playing") return;

    const scenario = state.scenarios[state.roundIndex];
    const isCorrect = answer === scenario.answer;
    const correctButton = elements["action-list"].querySelector(`[data-answer="${scenario.answer}"]`);

    state.phase = "feedback";
    for (const actionButton of elements["action-list"].querySelectorAll("button")) {
        actionButton.disabled = true;
    }

    correctButton.classList.add("is-correct");
    if (!isCorrect) button.classList.add("is-wrong");

    elements["tip-line"].hidden = true;
    elements["feedback"].hidden = false;
    elements["feedback"].dataset.result = isCorrect ? "correct" : "wrong";

    if (isCorrect) {
        state.correctAnswers += 1;
        state.streak += 1;
        const localCoinReward = state.streak >= 3 ? 30 : 20;
        const localXpReward = state.streak >= 3 ? 40 : 30;

        elements["feedback-title"].textContent =
            state.streak >= 3 ? "¡Correcto! Racha de aciertos 🔥" : "¡Muy bien! Decisión correcta";
        elements["feedback-copy"].textContent =
            `${scenario.explanation} Ganaste ${localCoinReward} monedas y ${localXpReward} XP.`;
        elements["streak"].textContent = `🔥 Racha: ${state.streak}`;

        if (window.MinijuegoHost && window.MinijuegoHost.isAndroid) {
            window.MinijuegoHost.submitAnswer(state.sessionId, scenario.id, state.roundIndex + 1, answer);
        } else {
            state.rewards.coins += localCoinReward;
            state.rewards.xp += localXpReward;
            profile.coins += localCoinReward;
            profile.xp += localXpReward;
            saveProfile();
            renderProfile();
        }
    } else {
        state.streak = 0;
        elements["feedback-title"].textContent = "Esta vez no 🌱";
        elements["feedback-copy"].textContent =
            `La mejor opción era ${scenario.answer === "REGAR" ? "regar" : "esperar y monitorear"}. ${scenario.explanation}`;
        elements["streak"].textContent = "🔥 Racha: 0";

        if (window.MinijuegoHost && window.MinijuegoHost.isAndroid) {
            window.MinijuegoHost.submitAnswer(state.sessionId, scenario.id, state.roundIndex + 1, answer);
        }
    }

    elements["continue-button"].innerHTML =
        state.roundIndex === ROUND_COUNT - 1
            ? "Ver resultado <span aria-hidden=\"true\">→</span>"
            : "Siguiente ronda <span aria-hidden=\"true\">→</span>";
}

function continueGame() {
    if (state.phase !== "feedback") return;

    if (state.roundIndex === ROUND_COUNT - 1) {
        finishGame();
        return;
    }

    state.roundIndex += 1;
    renderQuestion();
}

function finishGame() {
    state.phase = "finished";
    elements["garden"].dataset.sky = "night";

    const score = state.correctAnswers;
    elements["result-heading"].textContent =
        score === ROUND_COUNT ? "¡Jardín perfecto!" : score >= 4 ? "¡Buen trabajo, jardinero!" : "¡Tu jardín puede crecer!";
    elements["result-score"].textContent = `Acertaste ${score} de ${ROUND_COUNT} rondas`;
    elements["earned-coins"].textContent = `+${state.rewards.coins}`;
    elements["earned-xp"].textContent = `+${state.rewards.xp}`;
    elements["result-message"].textContent =
        score === ROUND_COUNT
            ? "¡Todas tus plantas recibieron el cuidado ideal!"
            : "Sigue observando los sensores: cada ronda es una oportunidad para aprender.";
    elements["result-overlay"].hidden = false;
}

function handleAction(event) {
    const button = event.target.closest("button[data-action]");
    if (!button) return;

    const action = button.dataset.action;
    if (action === "start") {
        startGame();
    } else if (action === "answer") {
        submitAnswer(button.dataset.answer, button);
    } else if (action === "continue") {
        continueGame();
    }
}

function updateGameScale() {
    const viewportWidth = window.visualViewport ? window.visualViewport.width : window.innerWidth;
    const viewportHeight = window.visualViewport ? window.visualViewport.height : window.innerHeight;
    const scale = Math.min(1, Math.min(viewportWidth / DESIGN_WIDTH, viewportHeight / DESIGN_HEIGHT));

    const stage = document.querySelector(".game-stage");
    if (stage) {
        stage.style.setProperty("--game-scale", scale.toFixed(4));
    }

    const sceneWrapper = document.getElementById("scene-wrapper");
    if (sceneWrapper) {
        sceneWrapper.style.setProperty("transform", `scale(${scale})`);
        sceneWrapper.style.setProperty("transform-origin", "center center");
    }

    const app = document.getElementById("app");
    if (app) {
        app.style.width = `${DESIGN_WIDTH}px`;
        app.style.height = `${DESIGN_HEIGHT}px`;
        app.style.maxWidth = "none";
        app.style.maxHeight = "none";
        app.style.transform = "none";
    }

    const overlayCard = document.querySelector(".overlay-card");
    if (overlayCard) {
        overlayCard.style.width = `${Math.min(475, 475 * scale).toFixed(2)}px`;
        overlayCard.style.transform = "none";
    }
}

// Android Bridge Functions
window.setAndroidState = function(jsonInput) {
    try {
        const data = typeof jsonInput === "string" ? JSON.parse(jsonInput) : jsonInput;
        if (data) {
            if (Number.isFinite(data.coins)) profile.coins = Math.floor(data.coins);
            if (Number.isFinite(data.xp)) profile.xp = Math.floor(data.xp);
            renderProfile();
            if (data.dailyLimitReached) {
                showDailyLimitNotice();
            }
        }
    } catch (e) {
        console.error("Error setting Android state", e);
    }
};

window.onAnswerProcessed = function(jsonInput) {
    try {
        const data = typeof jsonInput === "string" ? JSON.parse(jsonInput) : jsonInput;
        if (data) {
            if (Number.isFinite(data.totalCoins)) profile.coins = Math.floor(data.totalCoins);
            if (Number.isFinite(data.totalXp)) profile.xp = Math.floor(data.totalXp);
            renderProfile();

            const awardedCoins = data.awardedCoins || 0;
            const awardedXp = data.awardedXp || 0;
            state.rewards.coins += awardedCoins;
            state.rewards.xp += awardedXp;

            const scenario = state.scenarios[state.roundIndex];
            if (data.isCorrect && scenario) {
                elements["feedback-copy"].textContent =
                    `${scenario.explanation} Ganaste ${awardedCoins} monedas y ${awardedXp} XP.`;
            }

            if (data.dailyLimitReached) {
                showDailyLimitNotice();
            }
        }
    } catch (e) {
        console.error("Error on answer processed", e);
    }
};

function init() {
    findElements();
    loadProfile();
    renderProfile();
    updateGameScale();
    document.addEventListener("click", handleAction);
    window.addEventListener("resize", updateGameScale);
    if (window.visualViewport) {
        window.visualViewport.addEventListener("resize", updateGameScale);
    }

    if (window.MinijuegoHost && typeof window.MinijuegoHost.requestInitState === "function") {
        window.MinijuegoHost.requestInitState();
    }
}

init();
