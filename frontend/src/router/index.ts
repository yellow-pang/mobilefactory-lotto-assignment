import { createRouter, createWebHistory } from "vue-router";
import EventView from "../views/EventView.vue";
import ResultView from "../views/ResultView.vue";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: "/", redirect: "/event" },
    { path: "/event", name: "event", component: EventView },
    { path: "/result", name: "result", component: ResultView },
  ],
});

export default router;
