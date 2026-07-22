# ⚙️ PescaBrasil - API RESTful (Back-end)

> **Esta é a API que alimenta o sistema. [Acesse o repositório da Interface (UI) aqui](https://github.com/patrickpriebe/pesca-brasil-ui)**

O PescaBrasil é um sistema full-stack. Este repositório contém o **Back-end**, desenhado para ser escalável, monitorável e altamente seguro. Nada de atalhos: a autenticação é própria, a infraestrutura é moderna e o código é limpo.

O motor da aplicação garante o gerenciamento de relações complexas do diário de capturas, catálogos de espécies, controle de equipamentos e mapeamento geográfico.

---

## 🏗️ Arquitetura, Infraestrutura e Stack

A API foi desenvolvida focando na integridade dos dados, proteção de rotas e monitoramento em tempo real de produção.

*   **Java & Spring Boot:** Arquitetura rigorosa separada em camadas (*Controllers, Services, Repositories*).
*   **Autenticação Customizada:** Sistema de login próprio e raiz. Envolve disparo de e-mail transacional para verificação e proteção das rotas com integração de **Captcha**, garantindo segurança sem depender de soluções de prateleira (como Firebase Auth).
*   **Gestão de Mídia:** Upload de imagens de capturas integrado diretamente via API com o **Cloudinary**.
*   **Render (DevOps):** Hospedagem da API Spring Boot, mantendo os serviços sempre disponíveis e rodando em nuvem.
*   **Supabase (PostgreSQL):** Banco de dados relacional hospedado na nuvem, garantindo integridade e performance nas consultas complexas do diário e catálogos.
*   **Sentry:** Monitoramento full-time. Qualquer exceção ou erro gerado na aplicação é capturado e enviado em tempo real para análise de causa raiz.

---

## 🎣 Domínios da Aplicação (Features)

1.  **Auth Nativo e Seguro:** Endpoints para criação de conta, envio de token por e-mail e validação rigorosa com Captcha.
2.  **Diário de Troféus:** CRUD completo para registro de pescarias, validando associações com rios, equipamentos, iscas e destino do peixe.
3.  **Catálogos Relacionais:** Endpoints estruturados para gerenciar Peixes, Iscas e Equipamentos, incluindo recomendações dinâmicas (qual isca usar para qual espécie).
4.  **Leis e Defesos:** Controle de períodos de restrição de pesca (piracema) por bacia hidrográfica.

---

Desenvolvido por: **Patrick**

Desenvolvedor de Software, apaixonado por código limpo, arquitetura backend e interfaces que fogem do comum.

🔗 [LinkedIn](https://www.linkedin.com/in/patrickpriebe/) | 💻 [GitHub](https://github.com/patrickpriebe)