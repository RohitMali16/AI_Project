
let activeSessionId = null;

const pageMessage = document.getElementById('pageMessage');

function showPageMessage(message, type = '') {
    pageMessage.textContent = message;
    pageMessage.className = `page-message ${type}`;
}

async function api(url, options = {}) {
    const response = await fetch(url, {
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json' },
        ...options
    });

    const data = await response.json().catch(() => ({}));

    if (response.status === 401) {
        window.location.replace('/');
        throw new Error('Please login again.');
    }

    if (!response.ok) {
        throw new Error(data.error || data.message || 'Request failed');
    }

    return data;
}

function makeElement(tag, className, text) {
    const element = document.createElement(tag);

    if (className) element.className = className;
    if (text !== undefined) element.textContent = text;

    return element;
}

function renderList(title, values) {
    const section = makeElement('div', 'evaluation-section');

    section.append(makeElement('h4', '', title));

    if (!Array.isArray(values) || values.length === 0) {
        section.append(makeElement('p', 'muted', 'No items provided.'));
        return section;
    }

    const list = makeElement('ul', 'evaluation-list');

    values.forEach(value => {
        list.append(makeElement('li', '', value));
    });

    section.append(list);
    return section;
}

async function checkLogin() {
    try {
        const user = await api('/api/auth/me');

        document.getElementById('userEmail').textContent = user.email;

        await loadStats();
        await loadHistory();

    } catch (error) {
        // Login page redirect is handled by api().
    }
}

// LOGOUT
document.getElementById('logoutBtn').addEventListener('click', async () => {
    try {
        await api('/api/auth/logout', {
            method: 'POST',
            body: '{}'
        });

        window.location.replace('/');

    } catch (error) {
        showPageMessage(error.message, 'error');
    }
});

// GENERATE INTERVIEW QUESTIONS
document.getElementById('generateForm').addEventListener('submit', async (event) => {
    event.preventDefault();

    const role = document.getElementById('role').value.trim();
    const output = document.getElementById('questions');
    const button = document.getElementById('generateButton');

    if (!role) return;

    button.disabled = true;
    output.textContent = 'Generating questions...';

    try {
        const data = await api('/api/questions/generate', {
            method: 'POST',
            body: JSON.stringify({ role })
        });

        activeSessionId = data.sessionId;
        output.replaceChildren();

        data.questions.forEach(item => {
            const card = makeElement('article', 'question-card');

            card.append(
                makeElement('h3', '', `Question ${item.index + 1}`),
                makeElement('p', 'question-text', item.question)
            );

            const answer = makeElement('textarea', 'answer-input');
            answer.placeholder = 'Write your answer here...';
            answer.rows = 4;

            const evaluateButton = makeElement(
                'button',
                'primary-button',
                'Evaluate my answer'
            );

            evaluateButton.type = 'button';

            const result = makeElement('div', 'evaluation-result');

            evaluateButton.addEventListener('click', async () => {
                if (!answer.value.trim()) {
                    result.textContent = 'Please write your answer first.';
                    return;
                }

                evaluateButton.disabled = true;
                result.textContent = 'AI is evaluating your answer...';

                try {
                    const response = await api('/api/answer/evaluate', {
                        method: 'POST',
                        body: JSON.stringify({
                            sessionId: activeSessionId,
                            questionIndex: item.index,
                            answer: answer.value.trim()
                        })
                    });

                    const evaluation = response.evaluation;

                    result.replaceChildren();

                    result.append(
                        makeElement('p', 'score-line',
                            `Your score: ${evaluation.score}/10`),
                        renderList('What you did well', evaluation.strengths),
                        renderList('Points you missed', evaluation.missingPoints),
                        renderList('Suggestions to improve', evaluation.suggestions)
                    );

                    const improved = makeElement('div', 'evaluation-section');

                    improved.append(
                        makeElement('h4', '', 'Improved sample answer'),
                        makeElement('p', '', evaluation.improvedAnswer || '')
                    );

                    result.append(improved);

                    showPageMessage('Answer evaluated successfully.', 'success');
                    await loadStats();

                } catch (error) {
                    result.textContent = error.message;
                } finally {
                    evaluateButton.disabled = false;
                }
            });

            card.append(answer, evaluateButton, result);
            output.append(card);
        });

        showPageMessage('Questions generated successfully.', 'success');

    } catch (error) {
        output.textContent = error.message;
        showPageMessage(error.message, 'error');
    } finally {
        button.disabled = false;
    }
});

// AI ASSISTANT
document.getElementById('assistantForm').addEventListener('submit', async (event) => {
    event.preventDefault();

    const topic = document.getElementById('topic').value.trim();
    const output = document.getElementById('explanation');
    const button = document.getElementById('explainButton');

    if (!topic) return;

    button.disabled = true;
    output.textContent = 'Thinking...';

    try {
        const data = await api('/api/assistant/explain', {
            method: 'POST',
            body: JSON.stringify({ topic })
        });

        output.textContent = data.explanation;

    } catch (error) {
        output.textContent = error.message;
    } finally {
        button.disabled = false;
    }
});

// DASHBOARD STATISTICS
async function loadStats() {
    const container = document.getElementById('stats');

    try {
        const data = await api('/api/dashboard/stats');

        container.replaceChildren();

        const cards = [
            ['Questions attempted', data.totalQuestionsAttempted ?? 0],
            ['Average score', `${data.averageScore ?? 0}/10`],
            ['Strong topics', (data.strongTopics || []).length],
            ['Topics to improve', (data.weakTopics || []).length]
        ];

        cards.forEach(([label, value]) => {
            const card = makeElement('div', 'stat-card');

            card.append(
                makeElement('span', '', label),
                makeElement('strong', '', String(value))
            );

            container.append(card);
        });

    } catch (error) {
        showPageMessage(error.message, 'error');
    }
}

document.getElementById('loadStats').addEventListener('click', loadStats);

// INTERVIEW HISTORY
async function loadHistory() {
    const container = document.getElementById('historyContent');

    container.textContent = 'Loading history...';

    try {
        const data = await api('/api/history');

        container.replaceChildren();

        if (!data.sessions || data.sessions.length === 0) {
            container.textContent = 'No interview history yet.';
            return;
        }

        data.sessions.forEach(session => {
            const card = makeElement('article', 'history-card');

            card.append(
                makeElement('h3', '', session.role),
                makeElement(
                    'p',
                    'muted',
                    session.createdAt
                        ? new Date(session.createdAt).toLocaleString()
                        : 'Date unavailable'
                )
            );

            (session.questions || []).forEach((question, index) => {
                const detail = makeElement('div', 'history-question');

                detail.append(
                    makeElement('p', '', `${index + 1}. ${question.question}`)
                );

                if (question.score !== null && question.score !== undefined) {
                    detail.append(
                        makeElement('p', 'score-line', `Score: ${question.score}/10`)
                    );
                }

                card.append(detail);
            });

            container.append(card);
        });

    } catch (error) {
        container.textContent = error.message;
    }
}

document.getElementById('loadHistory').addEventListener('click', loadHistory);

// START DASHBOARD
checkLogin();
