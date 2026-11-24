"""Textos da interface do Android (lançador, controles, drivers) em pt-BR com en/es/fr/de/it.

Gera android/.../Tr.java (a tabela) e, com --envolver, troca cada texto da lista por Tr.t("...") nos .java (só os que
ainda não estão envolvidos; chaves internas como "#auto" não estão na lista). O app escolhe pelo idioma do Android:
português usa o texto original; en/es/fr/de/it a tradução; qualquer outro idioma, o inglês.
A chave é o texto como está escrito no código Java (com os escapes \\" de lá).
Uso: python tools/traducoes.py [--envolver]
"""
import os
import re
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
JAVA = os.path.join(AQUI, '..', 'android', 'app', 'src', 'main', 'java', 'com', 'luisxl', 'gowcollection')

# (pt, en, es, fr, de, it)
T = [
    # ---- lançador: tela principal
    ("Os dois jogos remasterizados  ·  PS Vita", "Both remastered games  ·  PS Vita", "Los dos juegos remasterizados  ·  PS Vita",
     "Les deux jeux remasterisés  ·  PS Vita", "Beide Remaster-Spiele  ·  PS Vita", "Entrambi i giochi rimasterizzati  ·  PS Vita"),
    ("God of War  ·  Capítulo I  ·  2005", "God of War  ·  Chapter I  ·  2005", "God of War  ·  Capítulo I  ·  2005",
     "God of War  ·  Chapitre I  ·  2005", "God of War  ·  Kapitel I  ·  2005", "God of War  ·  Capitolo I  ·  2005"),
    ("God of War II  ·  Capítulo II  ·  2007", "God of War II  ·  Chapter II  ·  2007", "God of War II  ·  Capítulo II  ·  2007",
     "God of War II  ·  Chapitre II  ·  2007", "God of War II  ·  Kapitel II  ·  2007", "God of War II  ·  Capitolo II  ·  2007"),
    ("Jogar", "Play", "Jugar", "Jouer", "Spielen", "Gioca"),
    ("Permitir acesso", "Allow access", "Permitir acceso", "Autoriser l'accès", "Zugriff erlauben", "Consenti l'accesso"),
    ("Controles na tela", "On-screen controls", "Controles en pantalla", "Commandes tactiles", "Bildschirmsteuerung", "Comandi su schermo"),
    ("Apoie o projeto", "Support the project", "Apoya el proyecto", "Soutenez le projet", "Projekt unterstützen", "Sostieni il progetto"),
    ("Troféus", "Trophies", "Trofeos", "Trophées", "Trophäen", "Trofei"),
    ("Idioma: ", "Language: ", "Idioma: ", "Langue : ", "Sprache: ", "Lingua: "),
    ("Tela: proporção", "Screen: aspect ratio", "Pantalla: relación de aspecto", "Écran : format d'image", "Bildschirm: Seitenverhältnis",
     "Schermo: proporzioni"),
    ("Driver de vídeo", "Graphics driver", "Controlador de vídeo", "Pilote graphique", "Grafiktreiber", "Driver video"),
    ("O jogo lê a pasta do PC no armazenamento do celular: permita o acesso aos arquivos",
     "The game reads the PC folder from the phone storage: allow access to files",
     "El juego lee la carpeta del PC en el almacenamiento del móvil: permite el acceso a los archivos",
     "Le jeu lit le dossier du PC dans le stockage du téléphone : autorisez l'accès aux fichiers",
     "Das Spiel liest den PC-Ordner aus dem Telefonspeicher: Dateizugriff erlauben",
     "Il gioco legge la cartella del PC nella memoria del telefono: consenti l'accesso ai file"),
    ("Copie a pasta \\\"", "Copy the \\\"", "Copia la carpeta \\\"", "Copiez le dossier \\\"", "Kopiere den Ordner \\\"", "Copia la cartella \\\""),
    ("\\\" do PC para o celular  ·  falta: ", "\\\" folder from the PC to the phone  ·  missing: ", "\\\" del PC al móvil  ·  falta: ",
     "\\\" du PC vers le téléphone  ·  manquant : ", "\\\" vom PC auf das Telefon  ·  fehlt: ", "\\\" dal PC al telefono  ·  manca: "),
    ("Falta ", "Missing ", "Falta ", "Manquant : ", "Es fehlt ", "Manca "),
    (" na pasta ", " in the folder ", " en la carpeta ", " dans le dossier ", " im Ordner ", " nella cartella "),
    ("a pasta ", "the folder ", "la carpeta ", "le dossier ", "der Ordner ", "la cartella "),
    ("Voltar", "Back", "Volver", "Retour", "Zurück", "Indietro"),
    ("Selecionar", "Select", "Seleccionar", "Choisir", "Auswählen", "Seleziona"),
    ("Abrir", "Open", "Abrir", "Ouvrir", "Öffnen", "Apri"),
    ("Em uso", "In use", "En uso", "Utilisé", "Aktiv", "In uso"),
    ("Drivers novos da pasta drivers instalados", "New drivers from the drivers folder installed",
     "Instalados los controladores nuevos de la carpeta drivers", "Nouveaux pilotes du dossier drivers installés",
     "Neue Treiber aus dem Ordner drivers installiert", "Installati i nuovi driver della cartella drivers"),
    ("Sem seletor de arquivos: copie o .zip para a pasta drivers do jogo", "No file picker: copy the .zip into the game's drivers folder",
     "Sin selector de archivos: copia el .zip a la carpeta drivers del juego",
     "Pas de sélecteur de fichiers : copiez le .zip dans le dossier drivers du jeu",
     "Keine Dateiauswahl: Kopiere die .zip in den Ordner drivers des Spiels",
     "Nessun selettore di file: copia lo .zip nella cartella drivers del gioco"),
    ("Instalado e em uso no próximo jogo: ", "Installed, used from the next game: ", "Instalado y en uso en el próximo juego: ",
     "Installé, utilisé à la prochaine partie : ", "Installiert, aktiv ab dem nächsten Spiel: ", "Installato, in uso dalla prossima partita: "),
    ("Não instalou: ", "Not installed: ", "No se instaló: ", "Non installé : ", "Nicht installiert: ", "Non installato: "),
    ("Não foi possível abrir o link", "Could not open the link", "No se pudo abrir el enlace", "Impossible d'ouvrir le lien",
     "Link konnte nicht geöffnet werden", "Impossibile aprire il link"),
    # ---- abertura
    ("Port feito e mantido exclusivamente por Luis Santos \\\"DESANTA\\\"", "Port made and maintained exclusively by Luis Santos \\\"DESANTA\\\"",
     "Port hecho y mantenido exclusivamente por Luis Santos \\\"DESANTA\\\"", "Portage réalisé et maintenu exclusivement par Luis Santos \\\"DESANTA\\\"",
     "Portierung erstellt und gepflegt ausschließlich von Luis Santos \\\"DESANTA\\\"", "Port realizzato e mantenuto esclusivamente da Luis Santos \\\"DESANTA\\\""),
    ("Se você baixou em qualquer outro lugar fora do repositório oficial do GitHub, CUIDADO: pode conter vírus!",
     "If you downloaded this anywhere other than the official GitHub repository, BEWARE: it may contain viruses!",
     "Si lo descargaste en cualquier otro lugar fuera del repositorio oficial de GitHub, ¡CUIDADO: puede contener virus!",
     "Si vous l'avez téléchargé ailleurs que sur le dépôt officiel GitHub, ATTENTION : il peut contenir des virus !",
     "Wenn du es irgendwo anders als im offiziellen GitHub-Repository heruntergeladen hast, VORSICHT: Es kann Viren enthalten!",
     "Se l'hai scaricato da qualsiasi posto diverso dal repository ufficiale di GitHub, ATTENZIONE: può contenere virus!"),
    ("Não confie em APKVISION e em canais do YouTube que disponibilizam links com propagandas em sites duvidosos.",
     "Do not trust APKVISION or YouTube channels that share links full of ads on shady sites.",
     "No confíes en APKVISION ni en canales de YouTube que comparten enlaces con publicidad en sitios dudosos.",
     "Ne faites pas confiance à APKVISION ni aux chaînes YouTube qui partagent des liens pleins de pubs vers des sites douteux.",
     "Vertraue weder APKVISION noch YouTube-Kanälen, die Links mit Werbung auf zweifelhaften Seiten verbreiten.",
     "Non fidarti di APKVISION né dei canali YouTube che condividono link pieni di pubblicità su siti dubbi."),
    ("Toque para continuar", "Tap to continue", "Toca para continuar", "Touchez pour continuer", "Tippen zum Fortfahren", "Tocca per continuare"),
    # ---- apoie o projeto
    ("O port é gratuito e feito por uma pessoa só. Se ele te divertiu, ajude a mantê-lo vivo.",
     "The port is free and made by a single person. If you had fun, help keep it alive.",
     "El port es gratuito y lo hace una sola persona. Si te divirtió, ayuda a mantenerlo vivo.",
     "Le portage est gratuit et fait par une seule personne. S'il vous a plu, aidez-le à rester en vie.",
     "Die Portierung ist kostenlos und von einer einzigen Person gemacht. Wenn sie dir Spaß gemacht hat, hilf, sie am Leben zu halten.",
     "Il port è gratuito e fatto da una sola persona. Se ti ha divertito, aiuta a tenerlo vivo."),
    ("Pix na hora; a sua mensagem aparece na live", "Instant Pix (Brazil); your message shows up on the live stream",
     "Pix al instante (Brasil); tu mensaje aparece en el directo", "Pix instantané (Brésil) ; votre message s'affiche pendant le live",
     "Sofort per Pix (Brasilien); deine Nachricht erscheint im Livestream", "Pix immediato (Brasile); il tuo messaggio appare in live"),
    ("Cartão ou saldo do PayPal, de qualquer país", "Card or PayPal balance, from any country", "Tarjeta o saldo de PayPal, desde cualquier país",
     "Carte ou solde PayPal, depuis n'importe quel pays", "Karte oder PayPal-Guthaben, aus jedem Land", "Carta o saldo PayPal, da qualsiasi paese"),
    ("Ou leia o QR code com outro aparelho", "Or scan the QR code with another device", "O escanea el código QR con otro dispositivo",
     "Ou scannez le QR code avec un autre appareil", "Oder scanne den QR-Code mit einem anderen Gerät", "Oppure inquadra il QR code con un altro dispositivo"),
    # ---- diagnóstico do sistema (relatório salvo ao abrir o jogo e no lançador)
    ("Diagnóstico do Sistema", "System Diagnostic", "Diagnóstico del sistema", "Diagnostic du système", "Systemdiagnose",
     "Diagnostico del sistema"),
    ("Diagnóstico copiado para a área de transferência!", "Diagnostic copied to the clipboard!", "Diagnóstico copiado al portapapeles!",
     "Diagnostic copié dans le presse-papiers !", "Diagnose in die Zwischenablage kopiert!", "Diagnostico copiato negli appunti!"),
    ("Compartilhar diagnóstico", "Share diagnostic", "Compartir diagnóstico", "Partager le diagnostic", "Diagnose teilen",
     "Condividi diagnostico"),
    ("Copiar", "Copy", "Copiar", "Copier", "Kopieren", "Copia"),
    ("Compartilhar", "Share", "Compartir", "Partager", "Teilen", "Condividi"),
    ("Fechar", "Close", "Cerrar", "Fermer", "Schließen", "Chiudi"),
    # ---- troféus
    ("Troféus de ", "Trophies of ", "Trofeos de ", "Trophées de ", "Trophäen von ", "Trofei di "),
    (" zerados neste celular (o antigo ficou em unlocked.txt.bak)", " reset on this phone (the old file is unlocked.txt.bak)",
     " reiniciados en este móvil (el anterior quedó en unlocked.txt.bak)", " réinitialisés sur ce téléphone (l'ancien est dans unlocked.txt.bak)",
     " auf diesem Telefon zurückgesetzt (die alte Datei ist unlocked.txt.bak)", " azzerati su questo telefono (il vecchio è in unlocked.txt.bak)"),
    ("Não deu para zerar", "Could not reset", "No se pudo reiniciar", "Réinitialisation impossible", "Zurücksetzen fehlgeschlagen", "Impossibile azzerare"),
    ("Nenhum troféu encontrado em game/PCSA00126/sce_sys/trophy", "No trophies found in game/PCSA00126/sce_sys/trophy",
     "No se encontraron trofeos en game/PCSA00126/sce_sys/trophy", "Aucun trophée trouvé dans game/PCSA00126/sce_sys/trophy",
     "Keine Trophäen in game/PCSA00126/sce_sys/trophy gefunden", "Nessun trofeo trovato in game/PCSA00126/sce_sys/trophy"),
    (" de ", " of ", " de ", " sur ", " von ", " di "),
    ("Toque de novo para zerar", "Tap again to reset", "Toca de nuevo para reiniciar", "Touchez à nouveau pour réinitialiser",
     "Erneut tippen zum Zurücksetzen", "Tocca di nuovo per azzerare"),
    ("Zerar troféus", "Reset trophies", "Reiniciar trofeos", "Réinitialiser les trophées", "Trophäen zurücksetzen", "Azzera trofei"),
    ("Ganho", "Earned", "Obtenido", "Obtenu", "Erhalten", "Ottenuto"),
    ("Bronze", "Bronze", "Bronce", "Bronze", "Bronze", "Bronzo"),
    ("Prata", "Silver", "Plata", "Argent", "Silber", "Argento"),
    ("Ouro", "Gold", "Oro", "Or", "Gold", "Oro"),
    ("Platina", "Platinum", "Platino", "Platine", "Platin", "Platino"),
    ("Troféu oculto", "Hidden trophy", "Trofeo oculto", "Trophée caché", "Verborgene Trophäe", "Trofeo nascosto"),
    ("Continue jogando para descobrir", "Keep playing to find out", "Sigue jugando para descubrirlo", "Continuez à jouer pour le découvrir",
     "Spiele weiter, um es herauszufinden", "Continua a giocare per scoprirlo"),
    ("Todos já ganhos (vieram do PC): zere para ganhá-los de novo e ver o aviso no jogo",
     "All already earned (they came from the PC): reset them to earn them again and see the in-game banner",
     "Todos ya obtenidos (vinieron del PC): reinícialos para ganarlos otra vez y ver el aviso en el juego",
     "Tous déjà obtenus (venus du PC) : réinitialisez-les pour les regagner et voir l'annonce en jeu",
     "Alle schon erhalten (vom PC übernommen): Setze sie zurück, um sie erneut zu verdienen und die Meldung im Spiel zu sehen",
     "Tutti già ottenuti (arrivati dal PC): azzerali per riottenerli e vedere l'avviso nel gioco"),
    ("Você ganhou um troféu", "You earned a trophy", "Has obtenido un trofeo", "Vous avez obtenu un trophée", "Du hast eine Trophäe erhalten",
     "Hai ottenuto un trofeo"),
    # ---- idioma e tela
    ("Idioma", "Language", "Idioma", "Langue", "Sprache", "Lingua"),
    ("O jogo usa o idioma se tiver os textos dele; os troféus também", "The game uses the language if it has its texts; trophies too",
     "El juego usa el idioma si tiene sus textos; los trofeos también", "Le jeu utilise la langue s'il en a les textes ; les trophées aussi",
     "Das Spiel nutzt die Sprache, wenn es die Texte hat; Trophäen auch", "Il gioco usa la lingua se ne ha i testi; anche i trofei"),
    ("Jogo e troféus", "Game and trophies", "Juego y trofeos", "Jeu et trophées", "Spiel und Trophäen", "Gioco e trofei"),
    ("Jogo (os troféus ficam em inglês)", "Game (trophies stay in English)", "Juego (los trofeos quedan en inglés)",
     "Jeu (les trophées restent en anglais)", "Spiel (Trophäen bleiben auf Englisch)", "Gioco (i trofei restano in inglese)"),
    ("Tela", "Screen", "Pantalla", "Écran", "Bildschirm", "Schermo"),
    ("Este celular é ", "This phone is ", "Este móvil es ", "Ce téléphone est en ", "Dieses Telefon ist ", "Questo telefono è "),
    ("  ·  vale na próxima abertura do jogo", "  ·  applies the next time the game starts", "  ·  se aplica la próxima vez que abras el juego",
     "  ·  appliqué au prochain lancement du jeu", "  ·  gilt beim nächsten Spielstart", "  ·  vale al prossimo avvio del gioco"),
    (" (vale na próxima abertura do jogo)", " (applies the next time the game starts)", " (se aplica la próxima vez que abras el juego)",
     " (appliqué au prochain lancement du jeu)", " (gilt beim nächsten Spielstart)", " (vale al prossimo avvio del gioco)"),
    ("Aumentar a proporção quebra os textos. Ainda em implementação.", "A wider aspect ratio breaks the texts. Still in progress.",
     "Aumentar la proporción rompe los textos. Todavía en desarrollo.", "Un format plus large casse les textes. Encore en cours.",
     "Ein breiteres Seitenverhältnis zerstört die Texte. Noch in Arbeit.", "Allargare le proporzioni rompe i testi. Ancora in sviluppo."),
    ("A tela inteira deste celular (", "This phone's whole screen (", "La pantalla completa de este móvil (", "Tout l'écran de ce téléphone (",
     "Der ganze Bildschirm dieses Telefons (", "Tutto lo schermo di questo telefono ("),
    ("A tela inteira deste celular", "This phone's whole screen", "La pantalla completa de este móvil", "Tout l'écran de ce téléphone",
     "Der ganze Bildschirm dieses Telefons", "Tutto lo schermo di questo telefono"),
    ("Faixas pretas nas laterais", "Black bars on the sides", "Franjas negras en los lados", "Bandes noires sur les côtés",
     "Schwarze Balken an den Seiten", "Bande nere ai lati"),
    ("Mais larga que esta tela: faixas em cima e embaixo", "Wider than this screen: bars at the top and bottom",
     "Más ancha que esta pantalla: franjas arriba y abajo", "Plus large que cet écran : bandes en haut et en bas",
     "Breiter als dieser Bildschirm: Balken oben und unten", "Più larga di questo schermo: bande sopra e sotto"),
    ("Proporção ", "Aspect ratio ", "Proporción ", "Format ", "Seitenverhältnis ", "Proporzioni "),
    # ---- drivers (tela)
    ("Automático", "Automatic", "Automático", "Automatique", "Automatisch", "Automatico"),
    ("Escolhe o driver certo para a ", "Picks the right driver for the ", "Elige el controlador adecuado para la ", "Choisit le bon pilote pour la ",
     "Wählt den passenden Treiber für die ", "Sceglie il driver giusto per la "),
    ("Driver do aparelho", "Device driver", "Controlador del dispositivo", "Pilote de l'appareil", "Gerätetreiber", "Driver del dispositivo"),
    ("O driver Vulkan que veio no celular", "The Vulkan driver that came with the phone", "El controlador Vulkan que vino con el móvil",
     "Le pilote Vulkan fourni avec le téléphone", "Der Vulkan-Treiber, der mit dem Telefon kam", "Il driver Vulkan fornito con il telefono"),
    ("Recomendado  ·  ", "Recommended  ·  ", "Recomendado  ·  ", "Recommandé  ·  ", "Empfohlen  ·  ", "Consigliato  ·  "),
    ("  ·  instalado", "  ·  installed", "  ·  instalado", "  ·  installé", "  ·  installiert", "  ·  installato"),
    ("  ·  baixa ao escolher", "  ·  downloads when chosen", "  ·  se descarga al elegirlo", "  ·  téléchargé au choix",
     "  ·  wird bei Auswahl geladen", "  ·  si scarica quando lo scegli"),
    ("Turnip que vem no app, funciona sem internet", "Turnip bundled with the app, works offline", "Turnip incluido en la app, funciona sin internet",
     "Turnip inclus dans l'appli, fonctionne hors ligne", "Turnip in der App enthalten, funktioniert offline", "Turnip incluso nell'app, funziona offline"),
    ("Importado", "Imported", "Importado", "Importé", "Importiert", "Importato"),
    ("Importar pacote .zip", "Import .zip package", "Importar paquete .zip", "Importer un paquet .zip", ".zip-Paket importieren", "Importa pacchetto .zip"),
    ("Pacote .zip de driver (AdrenoTools); ou copie o .zip para a pasta drivers do jogo",
     "Driver .zip package (AdrenoTools); or copy the .zip into the game's drivers folder",
     "Paquete .zip de controlador (AdrenoTools); o copia el .zip a la carpeta drivers del juego",
     "Paquet .zip de pilote (AdrenoTools) ; ou copiez le .zip dans le dossier drivers du jeu",
     "Treiber-.zip-Paket (AdrenoTools); oder kopiere die .zip in den Ordner drivers des Spiels",
     "Pacchetto .zip del driver (AdrenoTools); oppure copia lo .zip nella cartella drivers del gioco"),
    ("Procurar atualizações", "Check for updates", "Buscar actualizaciones", "Rechercher des mises à jour", "Nach Updates suchen", "Cerca aggiornamenti"),
    ("A lista de drivers ainda não foi baixada", "The driver list has not been downloaded yet", "La lista de controladores aún no se ha descargado",
     "La liste des pilotes n'a pas encore été téléchargée", "Die Treiberliste wurde noch nicht geladen", "L'elenco dei driver non è ancora stato scaricato"),
    ("Última consulta: ", "Last check: ", "Última consulta: ", "Dernière vérification : ", "Letzte Prüfung: ", "Ultimo controllo: "),
    ("Remover o driver em uso", "Remove the driver in use", "Quitar el controlador en uso", "Supprimer le pilote utilisé", "Aktiven Treiber entfernen",
     "Rimuovi il driver in uso"),
    ("Apaga ", "Deletes ", "Borra ", "Supprime ", "Löscht ", "Elimina "),
    (" e volta ao automático", " and goes back to automatic", " y vuelve al automático", " et revient à l'automatique", " und kehrt zu Automatisch zurück",
     " e torna all'automatico"),
    ("Procurando os drivers mais novos…", "Looking for the newest drivers…", "Buscando los controladores más nuevos…", "Recherche des pilotes les plus récents…",
     "Suche nach den neuesten Treibern…", "Ricerca dei driver più recenti…"),
    ("Não deu para consultar: ", "Could not check: ", "No se pudo consultar: ", "Vérification impossible : ", "Prüfung fehlgeschlagen: ", "Impossibile controllare: "),
    ("Em uso no próximo jogo: ", "Used from the next game: ", "En uso en el próximo juego: ", "Utilisé à la prochaine partie : ", "Aktiv ab dem nächsten Spiel: ",
     "In uso dalla prossima partita: "),
    ("Verificando os drivers…", "Checking the drivers…", "Comprobando los controladores…", "Vérification des pilotes…", "Treiber werden geprüft…",
     "Verifica dei driver…"),
    # ---- drivers (estado do automático e pacotes)
    (" (embutido)", " (bundled)", " (incluido)", " (intégré)", " (integriert)", " (incluso)"),
    ("o pacote não tem meta.json", "the package has no meta.json", "el paquete no tiene meta.json", "le paquet n'a pas de meta.json",
     "das Paket hat keine meta.json", "il pacchetto non ha meta.json"),
    ("meta.json ilegível (", "unreadable meta.json (", "meta.json ilegible (", "meta.json illisible (", "meta.json unlesbar (", "meta.json illeggibile ("),
    ("é um driver de Linux (para o Proton), não do Android", "it is a Linux driver (for Proton), not an Android one",
     "es un controlador de Linux (para Proton), no de Android", "c'est un pilote Linux (pour Proton), pas Android",
     "das ist ein Linux-Treiber (für Proton), kein Android-Treiber", "è un driver Linux (per Proton), non Android"),
    ("é um driver de Wayland, não do Android", "it is a Wayland driver, not an Android one", "es un controlador de Wayland, no de Android",
     "c'est un pilote Wayland, pas Android", "das ist ein Wayland-Treiber, kein Android-Treiber", "è un driver Wayland, non Android"),
    ("o meta.json não diz o libraryName", "meta.json does not give the libraryName", "el meta.json no indica el libraryName",
     "le meta.json n'indique pas le libraryName", "die meta.json nennt keinen libraryName", "il meta.json non indica il libraryName"),
    ("o pacote não tem ", "the package does not have ", "el paquete no tiene ", "le paquet n'a pas ", "das Paket enthält nicht ", "il pacchetto non ha "),
    (" não é uma biblioteca ARM64", " is not an ARM64 library", " no es una biblioteca ARM64", " n'est pas une bibliothèque ARM64",
     " ist keine ARM64-Bibliothek", " non è una libreria ARM64"),
    (" é de Linux (glibc), não do Android", " is for Linux (glibc), not Android", " es de Linux (glibc), no de Android", " est pour Linux (glibc), pas Android",
     " ist für Linux (glibc), nicht Android", " è per Linux (glibc), non Android"),
    ("PowerVR: a tela usa o driver do aparelho", "PowerVR: the screen uses the device driver", "PowerVR: la pantalla usa el controlador del dispositivo",
     "PowerVR : l'écran utilise le pilote de l'appareil", "PowerVR: Der Bildschirm nutzt den Gerätetreiber", "PowerVR: lo schermo usa il driver del dispositivo"),
    (": a tela usa o driver do aparelho (o PanVK do lado Android trava o gralloc do Mali)",
     ": the screen uses the device driver (PanVK's Android side locks up Mali's gralloc)",
     ": la pantalla usa el controlador del dispositivo (la parte Android de PanVK bloquea el gralloc del Mali)",
     " : l'écran utilise le pilote de l'appareil (la partie Android de PanVK bloque le gralloc du Mali)",
     ": Der Bildschirm nutzt den Gerätetreiber (die Android-Seite von PanVK blockiert Malis gralloc)",
     ": lo schermo usa il driver del dispositivo (la parte Android di PanVK blocca il gralloc del Mali)"),
    (" não abriu o jogo em ", " did not start the game in ", " no abrió el juego en ", " n'a pas lancé le jeu en ", " hat das Spiel nicht gestartet in ",
     " non ha avviato il gioco in "),
    (" tentativas: voltou o driver anterior", " attempts: the previous driver is back", " intentos: volvió el controlador anterior",
     " tentatives : retour au pilote précédent", " Versuchen: vorheriger Treiber wiederhergestellt", " tentativi: è tornato il driver precedente"),
    ("Nenhum driver a definir para ", "No driver to set for ", "Ningún controlador que definir para ", "Aucun pilote à définir pour ",
     "Kein Treiber festzulegen für ", "Nessun driver da impostare per "),
    (": a tela usa o driver do aparelho", ": the screen uses the device driver", ": la pantalla usa el controlador del dispositivo",
     " : l'écran utilise le pilote de l'appareil", ": Der Bildschirm nutzt den Gerätetreiber", ": lo schermo usa il driver del dispositivo"),
    ("Em dia (a consulta falhou: ", "Up to date (the check failed: ", "Al día (la consulta falló: ", "À jour (la vérification a échoué : ",
     "Aktuell (Prüfung fehlgeschlagen: ", "Aggiornato (il controllo non è riuscito: "),
    ("Em dia: ", "Up to date: ", "Al día: ", "À jour : ", "Aktuell: ", "Aggiornato: "),
    ("O RADV do Xclipse não está publicado agora: a tela usa o driver do aparelho",
     "The Xclipse RADV is not published right now: the screen uses the device driver",
     "El RADV de Xclipse no está publicado ahora: la pantalla usa el controlador del dispositivo",
     "Le RADV pour Xclipse n'est pas publié pour l'instant : l'écran utilise le pilote de l'appareil",
     "Das Xclipse-RADV ist gerade nicht veröffentlicht: Der Bildschirm nutzt den Gerätetreiber",
     "Il RADV per Xclipse ora non è pubblicato: lo schermo usa il driver del dispositivo"),
    ("Não deu para consultar (", "Could not check (", "No se pudo consultar (", "Vérification impossible (", "Prüfung fehlgeschlagen (", "Impossibile controllare ("),
    ("O driver recomendado ainda não foi publicado", "The recommended driver has not been published yet", "El controlador recomendado aún no se ha publicado",
     "Le pilote recommandé n'est pas encore publié", "Der empfohlene Treiber ist noch nicht veröffentlicht", "Il driver consigliato non è ancora stato pubblicato"),
    ("; usando ", "; using ", "; usando ", " ; utilisation de ", "; verwendet wird ", "; in uso "),
    ("Pulando ", "Skipping ", "Saltando ", "Ignoré : ", "Übersprungen: ", "Salto "),
    (", que falhou antes; usando ", ", which failed before; using ", ", que falló antes; usando ", ", qui a échoué avant ; utilisation de ",
     ", das vorher fehlschlug; verwendet wird ", ", che ha fallito prima; in uso "),
    ("Baixando ", "Downloading ", "Descargando ", "Téléchargement de ", "Lade ", "Download di "),
    ("Atualizado para ", "Updated to ", "Actualizado a ", "Mis à jour vers ", "Aktualisiert auf ", "Aggiornato a "),
    ("Usando ", "Using ", "Usando ", "Utilisation de ", "Verwendet: ", "In uso "),
    (" (em teste até o jogo abrir)", " (on trial until the game starts)", " (a prueba hasta que el juego abra)", " (à l'essai jusqu'au lancement du jeu)",
     " (im Test, bis das Spiel startet)", " (in prova finché il gioco non si avvia)"),
    ("não criou ", "could not create ", "no se pudo crear ", "impossible de créer ", "konnte nicht erstellen: ", "impossibile creare "),
    ("Não é um driver AdrenoTools: ", "Not an AdrenoTools driver: ", "No es un controlador AdrenoTools: ", "Ce n'est pas un pilote AdrenoTools : ",
     "Kein AdrenoTools-Treiber: ", "Non è un driver AdrenoTools: "),
    ("não moveu para ", "could not move to ", "no se pudo mover a ", "impossible de déplacer vers ", "konnte nicht verschieben nach ",
     "impossibile spostare in "),
    ("o manifest.json do pacote não tem a metade android", "the package manifest.json has no android half",
     "el manifest.json del paquete no tiene la parte android", "le manifest.json du paquet n'a pas la partie android",
     "die manifest.json des Pakets hat keinen Android-Teil", "il manifest.json del pacchetto non ha la parte android"),
    ("pacote grande demais", "package too large", "paquete demasiado grande", "paquet trop volumineux", "Paket zu groß", "pacchetto troppo grande"),
    ("Não baixou: ", "Download failed: ", "No se descargó: ", "Échec du téléchargement : ", "Download fehlgeschlagen: ", "Download non riuscito: "),
    ("nenhuma fonte respondeu", "no source answered", "ninguna fuente respondió", "aucune source n'a répondu", "keine Quelle hat geantwortet", "nessuna fonte ha risposto"),
    (" (teste)", " (test)", " (prueba)", " (test)", " (Test)", " (test)"),
    ("8 Gen 2 no One UI", "8 Gen 2 on One UI", "8 Gen 2 en One UI", "8 Gen 2 sous One UI", "8 Gen 2 unter One UI", "8 Gen 2 su One UI"),
    ("todas as Adreno", "all Adreno", "todas las Adreno", "toutes les Adreno", "alle Adreno", "tutte le Adreno"),
    ("Android + Linux · todas as Adreno", "Android + Linux · all Adreno", "Android + Linux · todas las Adreno", "Android + Linux · toutes les Adreno",
     "Android + Linux · alle Adreno", "Android + Linux · tutte le Adreno"),
    ("Turnip · todas as Adreno", "Turnip · all Adreno", "Turnip · todas las Adreno", "Turnip · toutes les Adreno", "Turnip · alle Adreno", "Turnip · tutte le Adreno"),
    ("Banners-Turnip · Adreno 710/720/722 (teste)", "Banners-Turnip · Adreno 710/720/722 (test)", "Banners-Turnip · Adreno 710/720/722 (prueba)",
     "Banners-Turnip · Adreno 710/720/722 (test)", "Banners-Turnip · Adreno 710/720/722 (Test)", "Banners-Turnip · Adreno 710/720/722 (test)"),
    ("Banners-Turnip · 8 Gen 2 no One UI", "Banners-Turnip · 8 Gen 2 on One UI", "Banners-Turnip · 8 Gen 2 en One UI", "Banners-Turnip · 8 Gen 2 sous One UI",
     "Banners-Turnip · 8 Gen 2 unter One UI", "Banners-Turnip · 8 Gen 2 su One UI"),
    ("GPU que não é Adreno", "Non-Adreno GPU", "GPU que no es Adreno", "GPU non Adreno", "GPU, die keine Adreno ist", "GPU non Adreno"),
    ("esta GPU", "this GPU", "esta GPU", "ce GPU", "diese GPU", "questa GPU"),
    (" (pelo chip ", " (from the chip ", " (por el chip ", " (d'après la puce ", " (laut Chip ", " (dal chip "),
    # ---- jogo e controles
    ("Prévia dos controles: o ômega no alto abre as opções", "Controls preview: the omega at the top opens the options",
     "Vista previa de los controles: la omega de arriba abre las opciones", "Aperçu des commandes : l'oméga en haut ouvre les options",
     "Steuerungsvorschau: Das Omega oben öffnet die Optionen", "Anteprima dei comandi: l'omega in alto apre le opzioni"),
    (" no APK", " in the APK", " en el APK", " dans l'APK", " im APK", " nell'APK"),
    ("Clássico", "Classic", "Clásico", "Classique", "Klassisch", "Classico"),
    ("Cristal", "Crystal", "Cristal", "Cristal", "Kristall", "Cristallo"),
    ("Minimalista", "Minimal", "Minimalista", "Minimaliste", "Minimalistisch", "Minimalista"),
    ("Arraste os controles  ·  toque no Ω para terminar", "Drag the controls  ·  tap Ω to finish", "Arrastra los controles  ·  toca Ω para terminar",
     "Faites glisser les commandes  ·  touchez Ω pour terminer", "Ziehe die Steuerelemente  ·  tippe auf Ω zum Beenden",
     "Trascina i comandi  ·  tocca Ω per finire"),
    ("Arraste os controles  ·  toque no ômega para terminar", "Drag the controls  ·  tap the omega to finish",
     "Arrastra los controles  ·  toca la omega para terminar", "Faites glisser les commandes  ·  touchez l'oméga pour terminer",
     "Ziehe die Steuerelemente  ·  tippe auf das Omega zum Beenden", "Trascina i comandi  ·  tocca l'omega per finire"),
    ("Escolher pasta", "Choose folder", "Elegir carpeta", "Choisir le dossier", "Ordner wählen", "Scegli cartella"),
    ("Pasta do jogo: ", "Game folder: ", "Carpeta del juego: ", "Dossier du jeu : ", "Spielordner: ", "Cartella del gioco: "),
    ("Essa pasta não tem: ", "That folder is missing: ", "A esa carpeta le falta: ", "Il manque dans ce dossier : ", "Diesem Ordner fehlt: ", "In questa cartella manca: "),
    ("Não deu para usar essa pasta", "Could not use that folder", "No se pudo usar esa carpeta", "Impossible d'utiliser ce dossier", "Dieser Ordner kann nicht verwendet werden", "Impossibile usare quella cartella"),
    ("Sem seletor de pastas neste celular", "No folder picker on this phone", "Este móvil no tiene selector de carpetas", "Pas de sélecteur de dossiers sur ce téléphone", "Keine Ordnerauswahl auf diesem Telefon", "Nessun selettore di cartelle su questo telefono"),
    ("Escolha a pasta \\\"", "Choose the \\\"", "Elige la carpeta \\\"", "Choisissez le dossier \\\"", "Wähle den Ordner \\\"", "Scegli la cartella \\\""),
    ("\\\" (a que tem game, data e shaders)", "\\\" folder (the one with game, data and shaders)", "\\\" (la que tiene game, data y shaders)", "\\\" (celui qui contient game, data et shaders)", "\\\" (der mit game, data und shaders)", "\\\" (quella con game, data e shaders)"),
    ('work.bin incompleto', 'incomplete work.bin', 'work.bin incompleto', 'work.bin incomplet', 'work.bin unvollständig', 'work.bin incompleto'),
    ('O work.bin precisa ter 512 bytes', 'The work.bin must be 512 bytes', 'El work.bin debe tener 512 bytes', 'Le work.bin doit faire 512 octets', 'Die work.bin muss 512 Bytes haben', 'Il work.bin deve essere di 512 byte'),
    ('work.bin de outro jogo: ', 'work.bin of another game: ', 'work.bin de otro juego: ', "work.bin d'un autre jeu : ", 'work.bin eines anderen Spiels: ', 'work.bin di un altro gioco: '),
    ('O PKG não é o americano (PCSA00126) ou está incompleto', 'The PKG is not the US one (PCSA00126) or is incomplete', 'El PKG no es el estadounidense (PCSA00126) o está incompleto', "Le PKG n'est pas l'américain (PCSA00126) ou est incomplet", 'Das PKG ist nicht das US-PKG (PCSA00126) oder unvollständig', 'Il PKG non è quello americano (PCSA00126) o è incompleto'),
    ('PKG ilegível', 'Unreadable PKG', 'PKG ilegible', 'PKG illisible', 'PKG unlesbar', 'PKG illeggibile'),
    ('Não é um PKG', 'Not a PKG', 'No es un PKG', "Ce n'est pas un PKG", 'Kein PKG', 'Non è un PKG'),
    ('PKG de outro jogo: ', 'PKG of another game: ', 'PKG de otro juego: ', "PKG d'un autre jeu : ", 'PKG eines anderen Spiels: ', 'PKG di un altro gioco: '),
    ('Conferindo o PKG', 'Checking the PKG', 'Comprobando el PKG', 'Vérification du PKG', 'PKG wird geprüft', 'Verifica del PKG'),
    ('manifesto.bin ruim', 'bad manifesto.bin', 'manifesto.bin dañado', 'manifesto.bin invalide', 'manifesto.bin fehlerhaft', 'manifesto.bin non valido'),
    ('A chave não confere: o PKG ou o work.bin não são os do jogo americano', 'The key does not match: the PKG or work.bin is not from the US game', 'La clave no coincide: el PKG o el work.bin no son del juego estadounidense', 'La clé ne correspond pas : le PKG ou le work.bin ne sont pas ceux du jeu américain', 'Der Schlüssel passt nicht: PKG oder work.bin sind nicht vom US-Spiel', 'La chiave non corrisponde: il PKG o il work.bin non sono del gioco americano'),
    ('Parte incompleta: ', 'Incomplete part: ', 'Parte incompleta: ', 'Partie incomplète : ', 'Unvollständiger Teil: ', 'Parte incompleta: '),
    ('Pacote cortado em ', 'Package cut at ', 'Paquete cortado en ', 'Paquet coupé à ', 'Paket abgeschnitten bei ', 'Pacchetto troncato in '),
    ('Arquivo corrompido: ', 'Corrupted file: ', 'Archivo dañado: ', 'Fichier corrompu : ', 'Beschädigte Datei: ', 'File danneggiato: '),
    ('Tirando o jogo do PKG', 'Extracting the game from the PKG', 'Extrayendo el juego del PKG', 'Extraction du jeu du PKG', 'Spiel wird aus dem PKG entpackt', 'Estrazione del gioco dal PKG'),
    ('Decifrando o jogo (PFS)', 'Decrypting the game (PFS)', 'Descifrando el juego (PFS)', 'Déchiffrement du jeu (PFS)', 'Spiel wird entschlüsselt (PFS)', 'Decifratura del gioco (PFS)'),
    ('Não deu para tirar o jogo do PKG: ', 'Could not extract the game from the PKG: ', 'No se pudo extraer el juego del PKG: ', "Impossible d'extraire le jeu du PKG : ", 'Spiel konnte nicht aus dem PKG entpackt werden: ', 'Impossibile estrarre il gioco dal PKG: '),
    ('A release não tem o pacote ', 'The release does not have the package ', 'La versión no tiene el paquete ', "La version n'a pas le paquet ", 'Die Version hat kein Paket ', 'La versione non ha il pacchetto '),
    ('Baixando os dados do jogo', 'Downloading the game data', 'Descargando los datos del juego', 'Téléchargement des données du jeu', 'Spieldaten werden geladen', 'Download dei dati del gioco'),
    ('Download falhou: ', 'Download failed: ', 'Falló la descarga: ', 'Échec du téléchargement : ', 'Download fehlgeschlagen: ', 'Download non riuscito: '),
    ('Download incompleto: ', 'Incomplete download: ', 'Descarga incompleta: ', 'Téléchargement incomplet : ', 'Download unvollständig: ', 'Download incompleto: '),
    ('Baixando a atualização', 'Downloading the update', 'Descargando la actualización', 'Téléchargement de la mise à jour', 'Update wird geladen', "Download dell'aggiornamento"),
    ('A atualização não instalou: ', 'The update did not install: ', 'La actualización no se instaló: ', "La mise à jour ne s'est pas installée : ", 'Das Update wurde nicht installiert: ', "L'aggiornamento non è stato installato: "),
    ('Atualizando os dados do jogo', 'Updating the game data', 'Actualizando los datos del juego', 'Mise à jour des données du jeu', 'Spieldaten werden aktualisiert', 'Aggiornamento dei dati del gioco'),
    ('Falta o PKG ou o work.bin', 'The PKG or work.bin is missing', 'Falta el PKG o el work.bin', 'Il manque le PKG ou le work.bin', 'PKG oder work.bin fehlt', 'Manca il PKG o il work.bin'),
    ('Cancelado', 'Cancelled', 'Cancelado', 'Annulé', 'Abgebrochen', 'Annullato'),
    ('Conferindo a licença', 'Checking the license', 'Comprobando la licencia', 'Vérification de la licence', 'Lizenz wird geprüft', 'Verifica della licenza'),
    ('Procurando a versão mais nova', 'Looking for the newest version', 'Buscando la versión más nueva', 'Recherche de la version la plus récente', 'Suche nach der neuesten Version', 'Ricerca della versione più recente'),
    ('Ainda não há uma versão publicada no GitHub', 'There is no version published on GitHub yet', 'Todavía no hay una versión publicada en GitHub', "Aucune version publiée sur GitHub pour l'instant", 'Auf GitHub ist noch keine Version veröffentlicht', "Non c'è ancora una versione pubblicata su GitHub"),
    ('Instalando os dados do jogo', 'Installing the game data', 'Instalando los datos del juego', 'Installation des données du jeu', 'Spieldaten werden installiert', 'Installazione dei dati del gioco'),
    ('Instalando as texturas HD', 'Installing the HD textures', 'Instalando las texturas HD', 'Installation des textures HD', 'HD-Texturen werden installiert', 'Installazione delle texture HD'),
    ('Jogo instalado', 'Game installed', 'Juego instalado', 'Jeu installé', 'Spiel installiert', 'Gioco installato'),
    ('Não encontrado: toque para escolher', 'Not found: tap to choose', 'No encontrado: toca para elegir', 'Introuvable : touchez pour choisir', 'Nicht gefunden: zum Auswählen tippen', 'Non trovato: tocca per scegliere'),
    ('Instalar o jogo', 'Install the game', 'Instalar el juego', 'Installer le jeu', 'Spiel installieren', 'Installa il gioco'),
    ('O jogo sai do PKG americano (PCSA00126) e do work.bin da sua licença; os dados vêm cifrados do GitHub.', "The game comes from the US PKG (PCSA00126) and your license's work.bin; the data comes encrypted from GitHub.", 'El juego sale del PKG estadounidense (PCSA00126) y del work.bin de tu licencia; los datos vienen cifrados de GitHub.', 'Le jeu vient du PKG américain (PCSA00126) et du work.bin de votre licence ; les données arrivent chiffrées depuis GitHub.', 'Das Spiel kommt aus dem US-PKG (PCSA00126) und der work.bin deiner Lizenz; die Daten kommen verschlüsselt von GitHub.', 'Il gioco viene dal PKG americano (PCSA00126) e dal work.bin della tua licenza; i dati arrivano cifrati da GitHub.'),
    ('PKG do jogo (gowHD_PSV.pkg, 3,3 GB)', 'Game PKG (gowHD_PSV.pkg, 3.3 GB)', 'PKG del juego (gowHD_PSV.pkg, 3,3 GB)', 'PKG du jeu (gowHD_PSV.pkg, 3,3 Go)', 'Spiel-PKG (gowHD_PSV.pkg, 3,3 GB)', 'PKG del gioco (gowHD_PSV.pkg, 3,3 GB)'),
    ('Licença (work.bin)', 'License (work.bin)', 'Licencia (work.bin)', 'Licence (work.bin)', 'Lizenz (work.bin)', 'Licenza (work.bin)'),
    ('Texturas HD (+1,7 GB)', 'HD textures (+1.7 GB)', 'Texturas HD (+1,7 GB)', 'Textures HD (+1,7 Go)', 'HD-Texturen (+1,7 GB)', 'Texture HD (+1,7 GB)'),
    ('Cancelar', 'Cancel', 'Cancelar', 'Annuler', 'Abbrechen', 'Annulla'),
    ('Instalar', 'Install', 'Instalar', 'Installer', 'Installieren', 'Installa'),
    ('Preparando', 'Preparing', 'Preparando', 'Préparation', 'Vorbereitung', 'Preparazione'),
    ('Já tenho a pasta copiada do PC', 'I already have the folder copied from the PC', 'Ya tengo la carpeta copiada del PC', "J'ai déjà le dossier copié du PC", 'Ich habe den Ordner schon vom PC kopiert', 'Ho già la cartella copiata dal PC'),
    ('Permita instalar apps deste app para receber a atualização', 'Allow this app to install apps to get the update', 'Permite que esta app instale apps para recibir la actualización', 'Autorisez cette appli à installer des applis pour recevoir la mise à jour', 'Erlaube dieser App, Apps zu installieren, um das Update zu erhalten', "Consenti a questa app di installare app per ricevere l'aggiornamento"),
    ('Sem seletor de arquivos neste celular', 'No file picker on this phone', 'Este móvil no tiene selector de archivos', 'Pas de sélecteur de fichiers sur ce téléphone', 'Keine Dateiauswahl auf diesem Telefon', 'Nessun selettore di file su questo telefono'),
    ('Abra o arquivo pelo armazenamento do celular (não por Recentes ou Downloads)', 'Open the file from the phone storage (not from Recent or Downloads)', 'Abre el archivo desde el almacenamiento del móvil (no desde Recientes o Descargas)', 'Ouvrez le fichier depuis le stockage du téléphone (pas depuis Récents ou Téléchargements)', 'Öffne die Datei über den Telefonspeicher (nicht über Zuletzt oder Downloads)', 'Apri il file dalla memoria del telefono (non da Recenti o Download)'),
    ('Texturas HD', 'HD textures', 'Texturas HD', 'Textures HD', 'HD-Texturen', 'Texture HD'),
    ('Toque de novo para apagar as texturas HD (1,7 GB)', 'Tap again to delete the HD textures (1.7 GB)', 'Toca de nuevo para borrar las texturas HD (1,7 GB)', 'Touchez encore pour supprimer les textures HD (1,7 Go)', 'Erneut tippen, um die HD-Texturen zu löschen (1,7 GB)', 'Tocca di nuovo per eliminare le texture HD (1,7 GB)'),
    ('Instaladas: toque para apagar e liberar 1,7 GB', 'Installed: tap to delete and free 1.7 GB', 'Instaladas: toca para borrar y liberar 1,7 GB', 'Installées : touchez pour supprimer et libérer 1,7 Go', 'Installiert: tippen zum Löschen (1,7 GB frei)', 'Installate: tocca per eliminare e liberare 1,7 GB'),
    ('Não instaladas: toque para baixar e instalar (+1,7 GB)', 'Not installed: tap to download and install (+1.7 GB)', 'No instaladas: toca para descargar e instalar (+1,7 GB)', 'Non installées : touchez pour télécharger et installer (+1,7 Go)', 'Nicht installiert: tippen zum Laden und Installieren (+1,7 GB)', 'Non installate: tocca per scaricare e installare (+1,7 GB)'),
    ('Apagando as texturas HD', 'Deleting the HD textures', 'Borrando las texturas HD', 'Suppression des textures HD', 'HD-Texturen werden gelöscht', 'Eliminazione delle texture HD'),
    ('Instalado pela pasta do PC: copie data/texpack do PC', 'Installed from the PC folder: copy data/texpack from the PC', 'Instalado con la carpeta del PC: copia data/texpack del PC', 'Installé depuis le dossier du PC : copiez data/texpack du PC', 'Über den PC-Ordner installiert: data/texpack vom PC kopieren', 'Installato dalla cartella del PC: copia data/texpack dal PC'),
    ('Tela: proporção e texturas HD', 'Screen: aspect ratio and HD textures', 'Pantalla: proporción y texturas HD', 'Écran : format et textures HD', 'Bildschirm: Seitenverhältnis und HD-Texturen', 'Schermo: proporzioni e texture HD'),
    (': a tela usa o driver do aparelho (o Turnip não vai bem nesta GPU)', ': the screen uses the device driver (Turnip does not run well on this GPU)', ': la pantalla usa el driver del dispositivo (Turnip no va bien en esta GPU)', " : l'écran utilise le pilote de l'appareil (Turnip ne fonctionne pas bien sur ce GPU)", ': der Bildschirm nutzt den Gerätetreiber (Turnip läuft auf dieser GPU nicht gut)', ': lo schermo usa il driver del dispositivo (Turnip non va bene su questa GPU)'),
    ("Voltar ao lançador", "Back to launcher", "Volver al lanzador", "Retour au lanceur", "Zurück zum Launcher", "Torna al launcher"),
    ("Desfazer", "Undo", "Deshacer", "Annuler", "Rückgängig", "Annulla"),
    ("Continuar", "Resume", "Continuar", "Reprendre", "Fortsetzen", "Riprendi"),
    ("Opacidade   ", "Opacity   ", "Opacidad   ", "Opacité   ", "Deckkraft   ", "Opacità   "),
    ("Tamanho   ", "Size   ", "Tamaño   ", "Taille   ", "Größe   ", "Dimensione   "),
    ("Vibração   ", "Vibration   ", "Vibración   ", "Vibration   ", "Vibration   ", "Vibrazione   "),
    ("Visual   ", "Style   ", "Estilo   ", "Style   ", "Stil   ", "Stile   "),
    ("ligada", "on", "activada", "activée", "an", "attiva"),
    ("desligada", "off", "desactivada", "désactivée", "aus", "disattiva"),
    ("Mostrar controles", "Show controls", "Mostrar controles", "Afficher les commandes", "Steuerung anzeigen", "Mostra comandi"),
    ("Esconder controles", "Hide controls", "Ocultar controles", "Masquer les commandes", "Steuerung ausblenden", "Nascondi comandi"),
    ("Mudar posições", "Move controls", "Cambiar posiciones", "Déplacer les commandes", "Positionen ändern", "Sposta comandi"),
    ("Posições originais", "Default positions", "Posiciones originales", "Positions d'origine", "Standardpositionen", "Posizioni originali"),
    ("Sair do jogo", "Quit game", "Salir del juego", "Quitter le jeu", "Spiel beenden", "Esci dal gioco"),
]

# arquivo -> textos (a chave) que ele mostra; os outros textos do arquivo ficam como estão
POR_ARQUIVO = {
    'LauncherActivity.java': None,  # None = todos os da tabela
    'ControlesView.java': None,
    'GameActivity.java': None,
    'Drivers.java': None,
    'DriverCatalog.java': ["nenhuma fonte respondeu", " (teste)", "8 Gen 2 no One UI", "todas as Adreno", "Android + Linux · todas as Adreno",
                           "Turnip · todas as Adreno", "Banners-Turnip · Adreno 710/720/722 (teste)", "Banners-Turnip · 8 Gen 2 no One UI"],
    'GpuInfo.java': ["GPU que não é Adreno", "esta GPU", " (pelo chip "],
    'Pastas.java': ["a pasta "],
}


def java_str(s):
    return '"' + s + '"'  # já vem com os escapes do Java


def gerar_tr():
    linhas = []
    for row in T:
        assert len(row) == 6, row[0]
        linhas.append('        p(' + ', '.join(java_str(x) for x in row) + ');')
    corpo = '\n'.join(linhas)
    src = '''package com.luisxl.gowcollection;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// GERADO por tools/traducoes.py (edite lá): os textos da interface no idioma do Android. A chave é o texto em
// português; en/es/fr/de/it têm tradução e qualquer outro idioma cai no inglês.
final class Tr {
    private static final Map<String, String[]> T = new HashMap<>();
    private static final int IDX = indice(Locale.getDefault().getLanguage());

    private static int indice(String l) {
        switch (l) {
            case "pt": return -1;
            case "es": return 1;
            case "fr": return 2;
            case "de": return 3;
            case "it": return 4;
            default: return 0;
        }
    }

    // o idioma do Android no código do Vita (SceSystemParamLang), para o padrão do jogo e dos troféus
    static int codigoVita() {
        switch (Locale.getDefault().getLanguage()) {
            case "pt": return 17;
            case "es": return 3;
            case "fr": return 2;
            case "de": return 4;
            case "it": return 5;
            case "ru": return 8;
            case "ja": return 0;
            default: return 1;
        }
    }

    static String t(String pt) {
        if (IDX < 0 || pt == null) return pt;
        String[] v = T.get(pt);
        return v == null ? pt : v[IDX];
    }

    private static void p(String pt, String en, String es, String fr, String de, String it) {
        T.put(pt, new String[]{en, es, fr, de, it});
    }

    static {
''' + corpo + '''
    }

    private Tr() {}
}
'''
    open(os.path.join(JAVA, 'Tr.java'), 'w', encoding='utf-8', newline='\n').write(src)
    print('Tr.java:', len(T), 'textos')


def envolver():
    chaves = [r[0] for r in T]
    assert len(chaves) == len(set(chaves)), [c for c in chaves if chaves.count(c) > 1]
    for arq, lista in POR_ARQUIVO.items():
        p = os.path.join(JAVA, arq)
        s = open(p, encoding='utf-8').read()
        usar = chaves if lista is None else lista
        total = 0
        saida = []
        for linha in s.split('\n'):
            st = linha.strip()
            if st.startswith('//') or st.startswith('*') or 'Log.' in linha or re.search(r'\bcase\s+"', linha):
                saida.append(linha)
                continue
            for k in sorted(usar, key=len, reverse=True):  # as maiores primeiro (uma contém a outra)
                lit = java_str(k)
                pos = 0
                while True:
                    i = linha.find(lit, pos)
                    if i < 0:
                        break
                    antes = linha[:i]
                    # já envolvido, ou parte de outro literal (a aspa de abertura não é de verdade)
                    aspas = len(re.findall(r'(?<!\\)"', antes))  # as \" de dentro de um texto não contam
                    if antes.endswith('Tr.t(') or aspas % 2 == 1 or re.search(r'\.(equals|startsWith|endsWith|contains)\($', antes):
                        pos = i + len(lit)
                        continue
                    linha = antes + 'Tr.t(' + lit + ')' + linha[i + len(lit):]
                    total += 1
                    pos = i + len('Tr.t(') + len(lit) + 1
            saida.append(linha)
        open(p, 'w', encoding='utf-8', newline='\n').write('\n'.join(saida))
        print(arq, total, 'envolvidos')


gerar_tr()
if '--envolver' in sys.argv:
    envolver()
