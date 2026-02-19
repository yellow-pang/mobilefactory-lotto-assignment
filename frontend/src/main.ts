import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import PrimeVue from "primevue/config";
import Aura from "@primeuix/themes/aura";
import Button from "primevue/button";
import Card from "primevue/card";
import InputText from "primevue/inputtext";
import Message from "primevue/message";
import "primeicons/primeicons.css";

const app = createApp(App);

app.use(router);
app.use(PrimeVue, {
  theme: { preset: Aura },
});

app.component("Button", Button);
app.component("Card", Card);
app.component("InputText", InputText);
app.component("Message", Message);

app.mount("#app");
