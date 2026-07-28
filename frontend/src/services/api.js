// frontend/src/services/api.js

// API base URL
// Local Development  -> http://localhost:8080
// Production (Vercel)-> https://vendra-ox6b.onrender.com

const API_BASE_URL =
  import.meta.env.VITE_API_URL || "http://localhost:8080";

export const previewVendors = [
  {
    id: 101,
    name: "Acme Industrial",
    category: "Fabrication",
    rating: 4.8,
    complianceStatus: "COMPLIANT",
    riskScore: 12
  },
  {
    id: 102,
    name: "Apex Logistics",
    category: "Freight & Storage",
    rating: 4.6,
    complianceStatus: "COMPLIANT",
    riskScore: 18
  },
  {
    id: 103,
    name: "Nexus Electronics",
    category: "Components",
    rating: 4.2,
    complianceStatus: "PENDING_REVIEW",
    riskScore: 42
  },
  {
    id: 104,
    name: "Vanguard Materials",
    category: "Raw Metals",
    rating: 3.9,
    complianceStatus: "NON_COMPLIANT",
    riskScore: 68
  },
  {
    id: 105,
    name: "Horizon Solar",
    category: "Energy Systems",
    rating: 4.7,
    complianceStatus: "COMPLIANT",
    riskScore: 15
  }
];

export const previewOrders = [
  {
    id: 201,
    vendorName: "Acme Industrial",
    amount: 145000,
    status: "PENDING_L1",
    approverChain: "APPROVER_L1 > APPROVER_L2"
  },
  {
    id: 202,
    vendorName: "Apex Logistics",
    amount: 82000,
    status: "PENDING_L2",
    approverChain: "APPROVER_L1 > APPROVER_L2"
  },
  {
    id: 203,
    vendorName: "Nexus Electronics",
    amount: 34000,
    status: "APPROVED",
    approverChain: "APPROVER_L1"
  },
  {
    id: 204,
    vendorName: "Horizon Solar",
    amount: 210000,
    status: "DRAFT",
    approverChain: "APPROVER_L1 > APPROVER_L2"
  }
];

export const previewRagChunks = [
  {
    chunkId: 1001,
    sourceDocId: 1,
    vendorId: 101,
    sourceType: "PDF",
    content:
      "Section 8.1 Indemnification: Supplier agrees to indemnify, defend, and hold harmless Buyer from all claims, damages, or liabilities arising from performance or product defects.",
    score: 0.92,
    confidence: "HIGH"
  },
  {
    chunkId: 1002,
    sourceDocId: 1,
    vendorId: 101,
    sourceType: "PDF",
    content:
      "Section 12.4 Automatic Renewal: This Master Agreement shall automatically renew for successive 12-month periods unless either party provides written notice 60 days prior to expiration.",
    score: 0.78,
    confidence: "HIGH"
  },
  {
    chunkId: 1003,
    sourceDocId: 2,
    vendorId: 103,
    sourceType: "TEXT",
    content:
      "Section 5.2 Termination for Convenience: Buyer may terminate this agreement at any time upon 30 days written notice. Supplier receives payment for work delivered prior to notice.",
    score: 0.65,
    confidence: "MEDIUM"
  }
];

export async function request(endpoint, options = {}, token = null) {
  const headers = {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers || {})
  };

  const config = {
    ...options,
    headers
  };

  // Ensure endpoint starts with "/"
  const url = endpoint.startsWith("/")
    ? `${API_BASE_URL}${endpoint}`
    : `${API_BASE_URL}/${endpoint}`;

  try {
    const response = await fetch(url, config);

    if (!response.ok) {
      const errorText = await response.text();

      let message = "API request failed";

      try {
        const json = JSON.parse(errorText);
        message = json.message || json.error || message;
      } catch {
        if (errorText) {
          message = errorText;
        }
      }

      throw new Error(message);
    }

    if (response.status === 204) {
      return null;
    }

    return await response.json();
  } catch (error) {
    console.error("API Request Failed:", url, error);
    throw error;
  }
}
