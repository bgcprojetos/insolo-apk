# INSOLO – Aplicativo Android

Este projeto gera um APK Android que abre o sistema INSOLO hospedado em:

`https://bgcprojetos.github.io/Insolo/`

O formulário, o login e o banco continuam no site/Supabase. Por isso, um serviço lançado no aplicativo aparece no relatório aberto pelo site — e vice-versa.

## Antes de gerar o APK

1. Publique no GitHub Pages o `index.html` configurado com seu Supabase.
2. Abra no celular ou no navegador: `https://bgcprojetos.github.io/Insolo/`.
3. Crie um usuário e registre um serviço para confirmar que a página está funcionando.

O APK **não contém chave secreta** do Supabase: ele só carrega a página HTTPS já publicada.

## Gerar pelo Android Studio

1. Instale o Android Studio recente.
2. Escolha **Open** e selecione esta pasta `insolo_android_app`.
3. Quando solicitado, instale o Android SDK 36 e use **JDK 17**.
4. Espere a sincronização terminar.
5. Acesse **Build > Build APK(s)**.
6. O arquivo será criado em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Gerar pelo GitHub, sem Android Studio

1. Crie um repositório no GitHub para este projeto Android ou envie o conteúdo desta pasta a um repositório existente.
2. Confirme que a pasta `.github/workflows` também foi enviada.
3. Abra a aba **Actions** do repositório.
4. Escolha **Gerar APK INSOLO** e clique em **Run workflow**.
5. Ao terminar, abra a execução e baixe o artefato **INSOLO-APK-debug**.

## Instalar no Android

Copie `app-debug.apk` para o celular e abra o arquivo. O Android poderá pedir autorização para instalar aplicativos desse gerenciador de arquivos. Autorize apenas porque o APK foi gerado por você ou pelo seu repositório.

## O que o app faz

- Usa o mesmo login e banco Supabase do site.
- Permite assinatura pelo canvas do formulário.
- Mantém o usuário no aplicativo para os domínios confiáveis do INSOLO e Supabase.
- Abre links externos, e-mail e telefone no aplicativo apropriado.
- Bloqueia conexões HTTP e acesso a arquivos locais.
- Exibe uma tela de nova tentativa quando não houver internet.

## Limitações desta primeira versão

O aplicativo depende da internet para abrir a página e consultar o Supabase. Para uma versão com trabalho totalmente offline, fila de sincronização, câmera e notificações, é necessário criar o formulário nativamente (Flutter, Kotlin ou React Native), mantendo o mesmo Supabase.

O APK `debug` serve para teste e instalação interna. Para publicar na Play Store, o correto é gerar um AAB de produção assinado com uma chave privada própria.
