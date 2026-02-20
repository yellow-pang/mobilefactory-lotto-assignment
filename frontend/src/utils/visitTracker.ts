/**
 * 매일 최초 접속 추적 유틸리티
 */

const LAST_VISIT_DATE_KEY = "lotto_last_visit_date";

/**
 * 오늘이 최초 접속인지 확인
 * @returns true: 최초 접속, false: 재접속
 */
export function isFirstVisitToday(): boolean {
  const lastVisitDate = localStorage.getItem(LAST_VISIT_DATE_KEY);
  const today = new Date().toISOString().split("T")[0]; // YYYY-MM-DD 형식

  if (!lastVisitDate) {
    // 처음 접속
    return true;
  }

  if (lastVisitDate !== today) {
    // 다른 날에 처음 접속
    return true;
  }

  // 같은 날 재접속
  return false;
}

/**
 * 현재 방문 날짜 업데이트
 */
export function updateLastVisitDate(): void {
  const today = new Date().toISOString().split("T")[0];
  localStorage.setItem(LAST_VISIT_DATE_KEY, today);
}

/**
 * 최초 접속 여부를 확인하고 업데이트
 * @returns true: 최초 접속
 */
export function checkAndUpdateFirstVisit(): boolean {
  const isFirst = isFirstVisitToday();
  if (isFirst) {
    updateLastVisitDate();
  }
  return isFirst;
}
