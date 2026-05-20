import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    warmup: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '30s', target: 20 },
        { duration: '30s', target: 20 },
      ],
      gracefulRampDown: '10s',
    },
    spike_same_question: {
      executor: 'constant-arrival-rate',
      rate: 100,
      timeUnit: '1s',
      duration: '2m',
      preAllocatedVUs: 120,
      maxVUs: 300,
      startTime: '1m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<3000'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export function setup() {
  http.post(`${BASE_URL}/api/loadtest/seed?documents=5000&paragraphs=6`);
}

export default function () {
  const sameQuestion = __ITER % 5 === 0;
  const payload = JSON.stringify({
    query: sameQuestion
      ? 'How does Lumina handle high concurrency?'
      : `Summarize load-test document ${__VU}-${__ITER}`,
    sessionId: sameQuestion ? 'shared-session' : `session-${__VU}`,
    dedupeKey: sameQuestion ? 'same-question-hot-key' : undefined,
    maxTokens: 48,
    thinkMillis: 180,
    tokenMillis: 5,
  });

  const response = http.post(`${BASE_URL}/api/loadtest/chat/stream`, payload, {
    headers: { 'Content-Type': 'application/json' },
    timeout: '30s',
  });

  check(response, {
    'status is 200': (r) => r.status === 200,
    'sse done': (r) => r.body.includes('[DONE]'),
  });

  sleep(0.1);
}
