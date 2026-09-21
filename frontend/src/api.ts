import { httpApi, setHttpIdentity } from "./httpApi";
import { showcaseApi, setShowcaseIdentity } from "./showcaseApi";
export { ApiError } from "./errors";
export { money } from "./httpApi";
const showcase = import.meta.env.VITE_SHOWCASE === "true";
export const api: typeof httpApi = showcase ? showcaseApi : httpApi;
export const setIdentity = (id: string) => {
  if (showcase) setShowcaseIdentity(id);
  else setHttpIdentity(id);
};
