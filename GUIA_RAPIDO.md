# Guia rápido: gerar e instalar o APK INSOLO

## Método mais simples: GitHub Actions

1. Extraia este ZIP no computador.
2. No GitHub, crie um repositório chamado, por exemplo, `insolo-apk`.
3. Clique em **Add file > Upload files**.
4. Envie **todo o conteúdo** desta pasta, inclusive a pasta oculta `.github`.
5. Clique em **Commit changes**.
6. Abra a aba **Actions**.
7. Clique no fluxo **Gerar APK INSOLO**.
8. Clique em **Run workflow > Run workflow**.
9. Quando a execução ficar verde, abra-a e baixe **INSOLO-APK-debug**.
10. Extraia o ZIP baixado pelo GitHub: dentro estará `app-debug.apk`.
11. Envie esse APK para o celular e instale.

## Verificação final

Abra o app e entre com o mesmo e-mail e senha criados no site. Cadastre um serviço no APK. Depois abra `https://bgcprojetos.github.io/Insolo/` em outro navegador e gere o relatório: o registro deve aparecer porque os dois usam o mesmo Supabase.
