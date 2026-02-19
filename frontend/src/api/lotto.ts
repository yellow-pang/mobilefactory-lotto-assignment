import http from "./http";

/**
 * Request/Response types for Lotto API
 */

export interface ParticipateRequest {
  phone: string;
}

export interface ParticipateResponse {
  participantId: number;
  lottoNumber: string;
}

export interface ResultCheckRequest {
  phone: string;
}

export interface ResultCheckResponse {
  rank?: number | null;
  isWinner?: boolean | null;
  lottoNumber?: string;
  amount?: number | null;
  checkCount?: number;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error: {
    code: string;
    message: string;
  } | null;
}

/**
 * Lotto API service functions
 */

export const lottoApi = {
  /**
   * Check if event period is active
   */
  async checkEventActive(): Promise<boolean> {
    try {
      // Event API 호출 시도 - 성공하면 기간 내
      await http.get("/participations/check-period");
      return true;
    } catch (error: any) {
      // EVENT_NOT_ACTIVE 에러면 기한 외
      if (error?.code === "EVENT_NOT_ACTIVE") {
        return false;
      }
      throw error;
    }
  },

  /**
   * Check if announce period is active
   */
  async checkAnnounceActive(): Promise<boolean> {
    try {
      // Announce API 호출 시도 - 성공하면 기간 내
      await http.get("/results/check-period");
      return true;
    } catch (error: any) {
      // ANNOUNCE_NOT_ACTIVE 에러면 기한 외
      if (error?.code === "ANNOUNCE_NOT_ACTIVE") {
        return false;
      }
      throw error;
    }
  },

  /**
   * Participate in lotto event
   */
  async participate(request: ParticipateRequest): Promise<ParticipateResponse> {
    const response = await http.post<
      ParticipateRequest,
      ApiResponse<ParticipateResponse>
    >("/participations", request);
    if (response.success) {
      return response.data;
    }
    throw new Error(response.error?.message || "Failed to participate");
  },

  /**
   * Check result status
   */
  async checkResult(request: ResultCheckRequest): Promise<ResultCheckResponse> {
    const response = await http.post<
      ResultCheckRequest,
      ApiResponse<ResultCheckResponse>
    >("/results/check", request);
    if (response.success) {
      return response.data;
    }
    throw new Error(response.error?.message || "Failed to check result");
  },
};
