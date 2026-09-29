import { Ionicons } from '@expo/vector-icons';
import * as Haptics from 'expo-haptics';
import { LinearGradient } from 'expo-linear-gradient';
import { ComponentProps, ReactNode, useState } from 'react';
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  StyleProp,
  StyleSheet,
  Text,
  TextInput,
  TextInputProps,
  View,
  ViewStyle,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { couleurs, espace, ombre, rayon, typo } from './theme';

export type NomIcone = ComponentProps<typeof Ionicons>['name'];

export const vibrer = () => {
  Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light).catch(() => undefined);
};

/** Conteneur d'écran : marges de sécurité, défilement et barre d'action fixe en bas. */
export function Ecran({ children, pied, defilement = true, style }: {
  children: ReactNode;
  pied?: ReactNode;
  defilement?: boolean;
  style?: StyleProp<ViewStyle>;
}) {
  const marges = useSafeAreaInsets();
  const contenu = [{ paddingTop: marges.top + espace.m, paddingHorizontal: espace.xl, gap: espace.xl }, style];
  return (
    <View style={{ flex: 1, backgroundColor: couleurs.fond }}>
      {defilement ? (
        <ScrollView contentContainerStyle={[contenu, { paddingBottom: espace.xxl }]} keyboardShouldPersistTaps="handled"
                    showsVerticalScrollIndicator={false}>
          {children}
        </ScrollView>
      ) : (
        <View style={[{ flex: 1 }, contenu]}>{children}</View>
      )}
      {pied && (
        <View style={[styles.pied, { paddingBottom: marges.bottom + espace.m }]}>{pied}</View>
      )}
    </View>
  );
}

export function EnTete({ titre, onRetour, droite }: { titre: string; onRetour?: () => void; droite?: ReactNode }) {
  return (
    <View style={styles.entete}>
      {onRetour ? (
        <BoutonIcone icone="chevron-back" onPress={onRetour} libelle="Retour" />
      ) : <View style={{ width: 44 }} />}
      <Text style={[typo.h3, { flex: 1, textAlign: 'center' }]} numberOfLines={1}>{titre}</Text>
      {droite ?? <View style={{ width: 44 }} />}
    </View>
  );
}

export function BoutonIcone({ icone, onPress, libelle, teinte = couleurs.texte }: {
  icone: NomIcone;
  onPress: () => void;
  libelle: string;
  teinte?: string;
}) {
  return (
    <Pressable onPress={onPress} accessibilityRole="button" accessibilityLabel={libelle} hitSlop={8}
               style={({ pressed }) => [styles.boutonIcone, pressed && { opacity: 0.6 }]}>
      <Ionicons name={icone} size={22} color={teinte} />
    </Pressable>
  );
}

type Variante = 'primaire' | 'secondaire' | 'fantome' | 'danger';

export function Bouton({ titre, onPress, variante = 'primaire', icone, chargement, desactive, testID }: {
  titre: string;
  onPress: () => void;
  variante?: Variante;
  icone?: NomIcone;
  chargement?: boolean;
  desactive?: boolean;
  testID?: string;
}) {
  const inactif = desactive || chargement;
  const couleurTexte = variante === 'primaire' ? couleurs.blanc
    : variante === 'danger' ? couleurs.danger : couleurs.marque;
  const interieur = chargement
    ? <ActivityIndicator color={couleurTexte} />
    : (
      <View style={styles.boutonLigne}>
        {icone && <Ionicons name={icone} size={20} color={couleurTexte} />}
        <Text style={[styles.boutonTexte, { color: couleurTexte }]}>{titre}</Text>
      </View>
    );

  return (
    <Pressable
      testID={testID}
      accessibilityRole="button"
      accessibilityState={{ disabled: !!inactif }}
      disabled={inactif}
      onPress={() => {
        vibrer();
        onPress();
      }}
      style={({ pressed }) => [
        { opacity: inactif ? 0.5 : 1, transform: [{ scale: pressed ? 0.98 : 1 }] },
      ]}>
      {variante === 'primaire' ? (
        <LinearGradient colors={[couleurs.marque, couleurs.marqueFonce]} start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }}
                        style={[styles.bouton, !inactif && ombre.forte]}>
          {interieur}
        </LinearGradient>
      ) : (
        <View style={[styles.bouton,
          variante === 'secondaire' && { backgroundColor: couleurs.marqueClair },
          variante === 'danger' && { backgroundColor: couleurs.dangerClair }]}>
          {interieur}
        </View>
      )}
    </Pressable>
  );
}

export function Champ({ label, aide, prefixe, suffixe, erreur, ...props }: TextInputProps & {
  label: string;
  aide?: string;
  prefixe?: string;
  suffixe?: string;
  erreur?: boolean;
}) {
  const [focus, setFocus] = useState(false);
  return (
    <View style={{ gap: espace.s }}>
      <Text style={typo.label}>{label}</Text>
      <View style={[styles.champ,
        focus && { borderColor: couleurs.marque, boxShadow: `0px 0px 0px 4px ${couleurs.marqueClair}` },
        erreur && { borderColor: couleurs.danger }]}>
        {prefixe && <Text style={styles.affixe}>{prefixe}</Text>}
        <TextInput
          placeholderTextColor={couleurs.texte3}
          {...props}
          onFocus={(e) => { setFocus(true); props.onFocus?.(e); }}
          onBlur={(e) => { setFocus(false); props.onBlur?.(e); }}
          style={styles.champTexte}
        />
        {suffixe && <Text style={styles.affixe}>{suffixe}</Text>}
      </View>
      {aide && <Text style={typo.petit}>{aide}</Text>}
    </View>
  );
}

export function Carte({ children, style }: { children: ReactNode; style?: StyleProp<ViewStyle> }) {
  return <View style={[styles.carte, style]}>{children}</View>;
}

export function Message({ type, texte }: { type: 'erreur' | 'info' | 'succes'; texte: string }) {
  const ton = {
    erreur: { fond: couleurs.dangerClair, texte: couleurs.danger, icone: 'alert-circle' as const },
    info: { fond: couleurs.accentClair, texte: '#93600B', icone: 'information-circle' as const },
    succes: { fond: couleurs.succesClair, texte: couleurs.succes, icone: 'checkmark-circle' as const },
  }[type];
  return (
    <View accessibilityRole="alert" style={[styles.message, { backgroundColor: ton.fond }]}>
      <Ionicons name={ton.icone} size={20} color={ton.texte} />
      <Text style={{ flex: 1, color: ton.texte, fontSize: 14, lineHeight: 20, fontWeight: '500' }}>{texte}</Text>
    </View>
  );
}

export function Pastille({ texte, active }: { texte: string; active: boolean }) {
  return (
    <View style={[styles.pastille, { backgroundColor: active ? couleurs.succesClair : couleurs.fond }]}>
      <View style={[styles.point, { backgroundColor: active ? couleurs.succes : couleurs.texte3 }]} />
      <Text style={{ fontSize: 12, fontWeight: '600', color: active ? couleurs.succes : couleurs.texte2 }}>{texte}</Text>
    </View>
  );
}

/** Pictogramme dans une pastille ronde colorée. */
export function Pictogramme({ icone, taille = 56, fond = couleurs.marqueClair, teinte = couleurs.marque }: {
  icone: NomIcone;
  taille?: number;
  fond?: string;
  teinte?: string;
}) {
  return (
    <View style={{ width: taille, height: taille, borderRadius: taille / 2, backgroundColor: fond,
                   alignItems: 'center', justifyContent: 'center' }}>
      <Ionicons name={icone} size={taille * 0.46} color={teinte} />
    </View>
  );
}

export function Section({ titre, children, action }: { titre: string; children: ReactNode; action?: ReactNode }) {
  return (
    <View style={{ gap: espace.m }}>
      <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }}>
        <Text style={styles.sectionTitre}>{titre}</Text>
        {action}
      </View>
      {children}
    </View>
  );
}

const styles = StyleSheet.create({
  pied: {
    paddingHorizontal: espace.xl,
    paddingTop: espace.m,
    backgroundColor: couleurs.surface,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: couleurs.bord,
  },
  entete: { flexDirection: 'row', alignItems: 'center', gap: espace.s, marginHorizontal: -espace.s },
  boutonIcone: {
    width: 44,
    height: 44,
    borderRadius: rayon.rond,
    backgroundColor: couleurs.surface,
    alignItems: 'center',
    justifyContent: 'center',
    ...ombre.douce,
  },
  bouton: {
    minHeight: 56,
    borderRadius: rayon.m,
    paddingHorizontal: espace.xl,
    alignItems: 'center',
    justifyContent: 'center',
  },
  boutonLigne: { flexDirection: 'row', alignItems: 'center', gap: espace.s },
  boutonTexte: { fontSize: 16, fontWeight: '700', letterSpacing: 0.1 },
  champ: {
    flexDirection: 'row',
    alignItems: 'center',
    minHeight: 56,
    borderRadius: rayon.m,
    borderWidth: 1.5,
    borderColor: couleurs.bord,
    backgroundColor: couleurs.surface,
    paddingHorizontal: espace.l,
    gap: espace.s,
  },
  champTexte: { flex: 1, minWidth: 0, fontSize: 17, color: couleurs.texte, paddingVertical: espace.m, outlineStyle: 'none' } as never,
  affixe: { fontSize: 16, fontWeight: '600', color: couleurs.texte2 },
  carte: {
    backgroundColor: couleurs.surface,
    borderRadius: rayon.l,
    padding: espace.l,
    ...ombre.douce,
  },
  message: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: espace.s,
    padding: espace.m,
    borderRadius: rayon.s,
  },
  pastille: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: rayon.rond,
    alignSelf: 'flex-start',
  },
  point: { width: 6, height: 6, borderRadius: 3 },
  sectionTitre: { fontSize: 13, fontWeight: '700', color: couleurs.texte3, textTransform: 'uppercase', letterSpacing: 0.8 },
});
