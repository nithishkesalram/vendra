export const previewVendors = [
  { id: 101, name: "Acme Industrial", category: "Fabrication", rating: 4.8, complianceStatus: "COMPLIANT", riskScore: 12 },
  { id: 102, name: "Apex Logistics", category: "Freight & Storage", rating: 4.6, complianceStatus: "COMPLIANT", riskScore: 18 },
  { id: 103, name: "Nexus Electronics", category: "Components", rating: 4.2, complianceStatus: "PENDING_REVIEW", riskScore: 42 },
  { id: 104, name: "Vanguard Materials", category: "Raw Metals", rating: 3.9, complianceStatus: "NON_COMPLIANT", riskScore: 68 },
  { id: 105, name: "Horizon Solar", category: "Energy Systems", rating: 4.7, complianceStatus: "COMPLIANT", riskScore: 15 }
];

export const previewOrders = [
  { id: 201, vendorName: "Acme Industrial", amount: 145000, status: "PENDING_L1", approverChain: "APPROVER_L1 > APPROVER_L2" },
  { id: 202, vendorName: "Apex Logistics", amount: 82000, status: "PENDING_L2", approverChain: "APPROVER_L1 > APPROVER_L2" },
  { id: 203, vendorName: "Nexus Electronics", amount: 34000, status: "APPROVED", approverChain: "APPROVER_L1" },
  { id: 204, vendorName: "Horizon Solar", amount: 210000, status: "DRAFT", approverChain: "APPROVER_L1 > APPROVER_L2" }
];

export const previewRagChunks = [
  { chunkId: 1001, sourceDocId: 1, vendorId: 101, sourceType: "PDF", content: "Section 8.1 Indemnification: Supplier agrees to indemnify, defend, and hold harmless Buyer from all claims, damages, or liabilities arising from performance or product defects.", score: 0.92, confidence: "HIGH" },
  { chunkId: 1002, sourceDocId: 1, vendorId: 101, sourceType: "PDF", content: "Section 12.4 Automatic Renewal: This Master Agreement shall automatically renew for successive 12-month periods unless either party provides written notice 60 days prior to expiration.", score: 0.78, confidence: "HIGH" },
  { chunkId: 1003, sourceDocId: 2, vendorId: 103, sourceType: "TEXT", content: "Section 5.2 Termination for Convenience: Buyer may terminate this agreement at any time upon 30 days written notice. Supplier receives payment for work delivered prior to notice.", score: 0.65, confidence: "MEDIUM" }
];

export async function request(endpoint, options = {}, token = null) {
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers || {})
  };

  const config = {
    ...options,
    headers
  };

  try {
    const res = await fetch(endpoint, config);
    if (!res.ok) {
      const errText = await res.text();
      let message = 'API request failed';
      try {
        const parsed = JSON.parse(errText);
        message = parsed.message || parsed.error || message;
      } catch (e) {
        if (errText) message = errText;
      }
      throw new Error(message);
    }

    if (res.status === 204) return null;
    return await res.json();
  } catch (err) {
    console.warn(`Request to ${endpoint} failed:`, err);
    throw err;
  }
}
