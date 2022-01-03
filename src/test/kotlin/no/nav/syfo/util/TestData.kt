package no.nav.syfo.util

import no.nav.syfo.model.Arbeidsgiver
import no.nav.syfo.model.Diagnose
import no.nav.syfo.model.ForslagTilTiltak
import no.nav.syfo.model.FunksjonsOgArbeidsevne
import no.nav.syfo.model.Henvisning
import no.nav.syfo.model.Kontakt
import no.nav.syfo.model.Legeerklaering
import no.nav.syfo.model.Pasient
import no.nav.syfo.model.Plan
import no.nav.syfo.model.Prognose
import no.nav.syfo.model.ReceivedLegeerklaering
import no.nav.syfo.model.RuleInfo
import no.nav.syfo.model.Signatur
import no.nav.syfo.model.Status
import no.nav.syfo.model.Sykdomsopplysninger
import no.nav.syfo.model.ValidationResult
import java.time.LocalDateTime

val validationResult = ValidationResult(
    status = Status.INVALID,
    ruleHits = listOf(
        RuleInfo(
            ruleName = "BEHANDLER_IKKE_GYLDIG_I_HPR",
            messageForUser = "Den som skrev legeerklæringen manglet autorisasjon.",
            messageForSender = "Legeerklæringen kan ikke rettes, det må skrives en ny. Grunnet følgende:" +
                "Behandler er ikke gyldig i HPR på konsultasjonstidspunkt",
            ruleStatus = Status.INVALID
        )
    )
)

val legeerklaering = Legeerklaering(
    id = "12314",
    arbeidsvurderingVedSykefravaer = true,
    arbeidsavklaringspenger = true,
    yrkesrettetAttforing = false,
    uforepensjon = true,
    pasient = Pasient(
        fornavn = "Test",
        mellomnavn = "Testerino",
        etternavn = "Testsen",
        fnr = "12349812345",
        navKontor = "NAV Stockholm",
        adresse = "Oppdiktet veg 99",
        postnummer = 9999,
        poststed = "Stockholm",
        yrke = "Taco spesialist",
        arbeidsgiver = Arbeidsgiver(
            navn = "NAV IKT",
            adresse = "Sannergata 2",
            postnummer = 557,
            poststed = "Oslo"
        )
    ),
    sykdomsopplysninger = Sykdomsopplysninger(
        hoveddiagnose = Diagnose(
            tekst = "Fysikalsk behandling/rehabilitering",
            kode = "-57"
        ),
        bidiagnose = listOf(
            Diagnose(
                tekst = "Engstelig for hjertesykdom",
                kode = "K24"
            )
        ),
        arbeidsuforFra = LocalDateTime.now().minusDays(3),
        sykdomshistorie = "Tekst",
        statusPresens = "Tekst",
        borNavKontoretVurdereOmDetErEnYrkesskade = true,
        yrkesSkadeDato = LocalDateTime.now().minusDays(4)
    ),
    plan = Plan(
        utredning = null,
        behandling = Henvisning(
            tekst = "2 timer i uken med svømming",
            dato = LocalDateTime.now(),
            antattVentetIUker = 1
        ),
        utredningsplan = "Tekst",
        behandlingsplan = "Tekst",
        vurderingAvTidligerePlan = "Tekst",
        narSporreOmNyeLegeopplysninger = "Tekst",
        videreBehandlingIkkeAktueltGrunn = "Tekst"
    ),
    forslagTilTiltak = ForslagTilTiltak(
        behov = true,
        kjopAvHelsetjenester = true,
        reisetilskudd = false,
        aktivSykmelding = false,
        hjelpemidlerArbeidsplassen = true,
        arbeidsavklaringspenger = true,
        friskmeldingTilArbeidsformidling = false,
        andreTiltak = "Trenger taco i lunsjen",
        naermereOpplysninger = "Tacoen må bestå av ordentlige råvarer",
        tekst = "Pasienten har store problemer med fordøying av annen mat enn Taco"

    ),
    funksjonsOgArbeidsevne = FunksjonsOgArbeidsevne(
        vurderingFunksjonsevne = "Kan ikke spise annet enn Taco",
        inntektsgivendeArbeid = false,
        hjemmearbeidende = false,
        student = false,
        annetArbeid = "Reisende taco tester",
        kravTilArbeid = "Kun taco i kantina",
        kanGjenopptaTidligereArbeid = true,
        kanGjenopptaTidligereArbeidNa = true,
        kanGjenopptaTidligereArbeidEtterBehandling = true,
        kanTaAnnetArbeid = true,
        kanTaAnnetArbeidNa = true,
        kanTaAnnetArbeidEtterBehandling = true,
        kanIkkeGjenopptaNaverendeArbeid = "Spise annen mat enn Taco",
        kanIkkeTaAnnetArbeid = "Spise annen mat enn Taco"
    ),
    prognose = Prognose(
        vilForbedreArbeidsevne = true,
        anslattVarighetSykdom = "1 uke",
        anslattVarighetFunksjonsnedsetting = "2 uker",
        anslattVarighetNedsattArbeidsevne = "4 uker"
    ),
    arsakssammenheng = "Funksjonsnedsettelsen har stor betydning for at arbeidsevnen er nedsatt",
    andreOpplysninger = "Tekst",
    kontakt = Kontakt(
        skalKontakteBehandlendeLege = true,
        skalKontakteArbeidsgiver = true,
        skalKontakteBasisgruppe = false,
        kontakteAnnenInstans = null,
        onskesKopiAvVedtak = true
    ),
    tilbakeholdInnhold = false,
    pasientenBurdeIkkeVite = null,
    signatur = Signatur(
        dato = LocalDateTime.now().minusDays(1),
        navn = "Lege Legesen",
        adresse = "Legeveien 33",
        postnummer = "9999",
        poststed = "Stockholm",
        signatur = "Lege Legesen",
        tlfNummer = "98765432"
    ),
    signaturDato = LocalDateTime.now()
)

val receivedLegeerklaering = ReceivedLegeerklaering(
    legeerklaering = legeerklaering,
    personNrPasient = "12349812345",
    pasientAktoerId = "563214543534536",
    personNrLege = "04030350265",
    legeAktoerId = "563211231252525",
    navLogId = "2034012301aidn.1",
    msgId = "05565fb0-cd7e-410d-bc1f-e1e918df2eac",
    legekontorOrgNr = "998004993",
    legekontorOrgName = "Kule helsetjeneser",
    legekontorHerId = "342425",
    legekontorReshId = "23141",
    mottattDato = LocalDateTime.now(),
    fellesformat = "fellesformatet",
    tssid = ""

)
