// Minimal REST adapter: transactions are committed atomically and retried on contention.
export function encode(value) {
  if (value === null || value === undefined) return { nullValue: null };
  if (typeof value === "boolean") return { booleanValue: value };
  if (typeof value === "number") {
    return Number.isInteger(value)
      ? { integerValue: String(value) }
      : { doubleValue: value };
  }
  if (typeof value === "string") return { stringValue: value };
  if (Array.isArray(value)) {
    return { arrayValue: { values: value.map(encode) } };
  }
  return {
    mapValue: {
      fields: Object.fromEntries(
        Object.entries(value).map(([k, v]) => [k, encode(v)]),
      ),
    },
  };
}
export function decode(value) {
  if ("nullValue" in value) return null;
  if ("stringValue" in value) return value.stringValue;
  if ("integerValue" in value) return Number(value.integerValue);
  if ("doubleValue" in value) return value.doubleValue;
  if ("booleanValue" in value) return value.booleanValue;
  if ("timestampValue" in value) return Date.parse(value.timestampValue);
  if ("arrayValue" in value) return (value.arrayValue.values || []).map(decode);
  return Object.fromEntries(
    Object.entries(value.mapValue?.fields || {}).map((
      [k, v],
    ) => [k, decode(v)]),
  );
}
export class Firestore {
  constructor(project, accessToken, emulator = "") {
    this.root = `projects/${project}/databases/(default)/documents`;
    this.url = emulator || "https://firestore.googleapis.com/v1";
    this.accessToken = accessToken;
  }
  async request(path, body, method = "POST") {
    const res = await fetch(`${this.url}/${path}`, {
      method,
      signal: AbortSignal.timeout(20000),
      headers: {
        Authorization: `Bearer ${await this.accessToken()}`,
        "Content-Type": "application/json",
      },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    });
    if (res.status === 404) return null;
    const result = await res.json();
    if (!res.ok) {
      const e = new Error("Servicio de datos no disponible.");
      e.status = result.error?.status;
      throw e;
    }
    return result;
  }
  async get(path, transaction) {
    let doc;
    if (transaction) {
      const rows = await this.request(`${this.root}:batchGet`, {
        documents: [`${this.root}/${path}`],
        transaction,
      });
      doc = (Array.isArray(rows) ? rows : [rows]).find((row) => row?.found)
        ?.found;
    } else {
      doc = await this.request(`${this.root}/${path}`, undefined, "GET");
    }
    return doc ? decode({ mapValue: { fields: doc.fields } }) : null;
  }
  async set(path, value) {
    await this.request(`${this.root}:commit`, {
      writes: [{
        update: {
          name: `${this.root}/${path}`,
          fields: encode(value).mapValue.fields,
        },
      }],
    });
  }
  async remove(path) {
    await this.request(`${this.root}:commit`, {
      writes: [{ delete: `${this.root}/${path}` }],
    });
  }
  async list(collection, field, value, limit = 500) {
    const parts = collection.split("/");
    const collectionId = parts.pop();
    const parent = parts.length ? `${this.root}/${parts.join("/")}` : this.root;
    const query = { from: [{ collectionId }], limit };
    if (field) {
      query.where = {
        fieldFilter: {
          field: { fieldPath: field },
          op: "EQUAL",
          value: encode(value),
        },
      };
    }
    const rows = await this.request(`${parent}:runQuery`, {
      structuredQuery: query,
    });
    return (rows || []).filter((x) => x.document).map((x) => ({
      ...decode({ mapValue: { fields: x.document.fields } }),
      _path: x.document.name.split("/documents/")[1],
    }));
  }
  async atomic(work) {
    for (let attempt = 0; attempt < 5; attempt++) {
      const { transaction } = await this.request(
        `${this.root}:beginTransaction`,
        {},
      );
      const writes = [];
      try {
        const result = await work({
          get: (path) => this.get(path, transaction),
          set: (path, value) =>
            writes.push({
              update: {
                name: `${this.root}/${path}`,
                fields: encode(value).mapValue.fields,
              },
            }),
        });
        await this.request(`${this.root}:commit`, { transaction, writes });
        return result;
      } catch (error) {
        await this.request(`${this.root}:rollback`, { transaction }).catch(
          () => {},
        );
        if (error.status !== "ABORTED" || attempt === 4) throw error;
        await new Promise((resolve) =>
          setTimeout(
            resolve,
            Math.min(1500, 100 * 2 ** attempt) * (0.5 + Math.random()),
          )
        );
      }
    }
  }
}
