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
