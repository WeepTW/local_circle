import type { Problem } from "./types";
export class ApiError extends Error {
  constructor(public problem: Problem) {
    super(problem.detail);
  }
}
