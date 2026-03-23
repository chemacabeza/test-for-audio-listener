export interface TranscriptionResponse {
  id: number;
  filename: string;
  originalFileType: string | null;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  transcriptText: string | null;
  createdAt: string;
  updatedAt: string;
  audioDurationSeconds: number | null;
  errorMessage: string | null;
}

export interface ErrorResponse {
  status: number;
  error: string;
  message: string;
  timestamp: string;
}

const API_BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const error: ErrorResponse = await response.json().catch(() => ({
      status: response.status,
      error: response.statusText,
      message: 'An unexpected error occurred',
      timestamp: new Date().toISOString(),
    }));
    throw new Error(error.message || `Request failed with status ${response.status}`);
  }
  return response.json();
}

export async function uploadAudio(file: File): Promise<TranscriptionResponse> {
  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch(`${API_BASE}/transcriptions`, {
    method: 'POST',
    body: formData,
  });

  return handleResponse<TranscriptionResponse>(response);
}

export async function getTranscriptions(query?: string): Promise<TranscriptionResponse[]> {
  const params = query ? `?q=${encodeURIComponent(query)}` : '';
  const response = await fetch(`${API_BASE}/transcriptions${params}`);
  return handleResponse<TranscriptionResponse[]>(response);
}

export async function getTranscription(id: number): Promise<TranscriptionResponse> {
  const response = await fetch(`${API_BASE}/transcriptions/${id}`);
  return handleResponse<TranscriptionResponse>(response);
}

export async function deleteTranscription(id: number): Promise<void> {
  const response = await fetch(`${API_BASE}/transcriptions/${id}`, {
    method: 'DELETE',
  });
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Delete failed' }));
    throw new Error(error.message);
  }
}

export function getDownloadUrl(id: number): string {
  return `${API_BASE}/transcriptions/${id}/download`;
}
