# IDOL-Lab
A casual K-pop management &amp; trainee-life simulation game. IdoLab is a hybrid K-pop simulation game inspired by titles like Monthly Idol and Idol Manager. The game lets you experience both sides of the industry: the sharp suit of a CEO running an entertainment company, and the messy, chaotic, adrenaline-filled path of a trainee fighting to debut.

## Ejecutar en PC (CLI)
1. Asegúrate de tener Java 17+ instalado.
2. Compila el prototipo: `javac Main.java`
3. Inicia la versión de consola: `java Main`

## Probar en móvil o PC con navegador (modo web ligero)
1. Compila el prototipo: `javac Main.java`
2. Lanza el servidor: `java Main --web`
3. Abre el navegador del móvil (o de la PC) y entra a `http://<IP>:8080` (en la misma red). También funciona en el mismo equipo con `http://localhost:8080`.
4. Usa los enlaces de ayuda en la página de inicio para ejecutar acciones de CEO o de trainee desde el navegador.

### Notas móviles
- En Android puedes ejecutar `java Main --web` desde Termux y abrir el navegador en `http://localhost:8080`.
- El servidor expone HTML básico sin frameworks para que funcione en navegadores móviles sin instalar nada extra. La lógica del juego es compartida con la versión de consola.