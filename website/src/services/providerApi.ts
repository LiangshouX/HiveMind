/**
 * Provider Management API Service
 *
 * CRUD operations for model provider configurations.
 * Uses the fetch-based http client (not the legacy axios api.ts).
 */

import {
  getJson,
  postJson,
  putJson,
  deleteJson,
  patchJson,
} from "./http";

// ── Types ──

export interface ProviderVO {
  id: number;
  modelProviderId: string;
  providerName: string;
  modelProviderType: "SYSTEM" | "CUSTOM" | "LOCAL";
  isProviderActivated: boolean;
  isSystemBuiltIn: boolean;
  baseUrl: string;
  apiKeyMask: string;
  modelId: string;
  modelName: string;
  modelsJson: string;
  createTime: string;
  updateTime: string;
}

export interface ProviderDTO {
  modelProviderId?: string;
  providerName?: string;
  modelProviderType?: string;
  baseUrl?: string;
  apiKey?: string;
  modelId?: string;
  modelName?: string;
  modelsJson?: string;
  activated?: boolean;
}

export interface PageResult<T> {
  total: number;
  records: T[];
  current: number;
  size: number;
}

export interface ConnectionTestResult {
  reachable: boolean;
  latencyMs: number;
  discoveredModels: Array<{ id: string; name: string }>;
  errorMessage?: string;
}

// ── API Base ──

const PROVIDER_BASE = "/providers";

// ── API Functions ──

/**
 * List providers with pagination.
 * @param current Page number (1-based, default 1)
 * @param size Page size (default 10)
 */
export async function listProviders(
  current = 1,
  size = 50,
): Promise<PageResult<ProviderVO>> {
  return getJson<PageResult<ProviderVO>>(
    `${PROVIDER_BASE}?current=${current}&size=${size}`,
  );
}

/**
 * Get a single provider by ID.
 */
export async function getProvider(id: number): Promise<ProviderVO> {
  return getJson<ProviderVO>(`${PROVIDER_BASE}/${id}`);
}

/**
 * Create a new provider.
 */
export async function createProvider(dto: ProviderDTO): Promise<ProviderVO> {
  return postJson<ProviderVO>(PROVIDER_BASE, dto);
}

/**
 * Update an existing provider.
 */
export async function updateProvider(
  id: number,
  dto: ProviderDTO,
): Promise<ProviderVO> {
  return putJson<ProviderVO>(`${PROVIDER_BASE}/${id}`, dto);
}

/**
 * Delete a provider. System built-in providers cannot be deleted.
 */
export async function deleteProvider(id: number): Promise<boolean> {
  return deleteJson<boolean>(`${PROVIDER_BASE}/${id}`);
}

/**
 * Activate a provider.
 */
export async function activateProvider(id: number): Promise<boolean> {
  return patchJson<boolean>(
    `${PROVIDER_BASE}/${id}/activation?active=true`,
  );
}

/**
 * Deactivate a provider.
 */
export async function deactivateProvider(id: number): Promise<boolean> {
  return patchJson<boolean>(
    `${PROVIDER_BASE}/${id}/activation?active=false`,
  );
}

/**
 * Select a model for a provider.
 */
export async function selectModel(
  id: number,
  modelId: string,
  modelName: string,
): Promise<boolean> {
  return patchJson<boolean>(
    `${PROVIDER_BASE}/${id}/select-model?modelId=${encodeURIComponent(modelId)}&modelName=${encodeURIComponent(modelName)}`,
  );
}

/**
 * Test connection to a provider.
 */
export async function testConnection(
  dto: ProviderDTO,
): Promise<ConnectionTestResult> {
  return postJson<ConnectionTestResult>(`${PROVIDER_BASE}/test-connection`, dto);
}

/**
 * Get the current default (activated) model.
 */
export async function getDefaultModel(): Promise<ProviderVO> {
  return getJson<ProviderVO>(`${PROVIDER_BASE}/default-model`);
}

/**
 * Initialize built-in providers for the current user.
 * Returns the list of providers after initialization.
 */
export async function initializeBuiltIn(): Promise<ProviderVO[]> {
  return postJson<ProviderVO[]>(`${PROVIDER_BASE}/initialize-built-in`, {});
}
