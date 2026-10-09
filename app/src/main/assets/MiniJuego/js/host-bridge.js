(function () {
    const isAndroid = typeof window.AndroidBridge !== "undefined";

    window.MinijuegoHost = {
        isAndroid: isAndroid,
        requestInitState: function () {
            if (isAndroid && typeof window.AndroidBridge.requestInitState === "function") {
                try {
                    window.AndroidBridge.requestInitState();
                } catch (e) {
                    console.error("Error requesting init state from Android", e);
                }
            }
        },
        submitAnswer: function (sessionId, caseId, round, selectedAnswer) {
            if (isAndroid && typeof window.AndroidBridge.submitAnswer === "function") {
                try {
                    const payload = {
                        sessionId: sessionId,
                        caseId: caseId,
                        round: round,
                        selectedAnswer: selectedAnswer
                    };
                    window.AndroidBridge.submitAnswer(JSON.stringify(payload));
                } catch (e) {
                    console.error("Error submitting answer to Android", e);
                }
            }
        }
    };
})();
