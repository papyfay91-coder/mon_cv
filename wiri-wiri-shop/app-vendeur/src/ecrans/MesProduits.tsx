import { Ionicons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { useCallback, useEffect, useState } from 'react';
import { FlatList, Image, Pressable, RefreshControl, Share, StyleSheet, Text, useWindowDimensions, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { api, Boutique, formaterPrix, Produit, VITRINE_URL } from '../api';
import { couleurs, espace, ombre, rayon, typo } from '../theme';
import { BoutonIcone, Pastille, Pictogramme, vibrer } from '../ui';

export default function MesProduits({ boutique, onAjouter, onParametres }: {
  boutique: Boutique;
  onAjouter: () => void;
  onParametres: () => void;
}) {
  const marges = useSafeAreaInsets();
  // Largeur exacte d'une colonne : la dernière carte d'une ligne incomplète garde la même taille.
  const largeurCarte = (useWindowDimensions().width - espace.xl * 2 - espace.m) / 2;
  const [produits, setProduits] = useState<Produit[]>([]);
  const [chargement, setChargement] = useState(true);

  const charger = useCallback(async () => {
    setChargement(true);
    try {
      setProduits(await api.produits());
    } finally {
      setChargement(false);
    }
  }, []);

  useEffect(() => {
    charger();
  }, [charger]);

  const basculer = async (p: Produit) => {
    vibrer();
    const maj = await api.basculerProduit(p.id, !p.actif);
    setProduits((liste) => liste.map((x) => (x.id === maj.id ? maj : x)));
  };

  const partager = () =>
    Share.share({ message: `Découvrez ma boutique ${boutique.nomVendeur} : ${VITRINE_URL}/boutique/${boutique.id}` });

  const enVente = produits.filter((p) => p.actif).length;

  const entete = (
    <View style={{ gap: espace.xl, marginBottom: espace.l }}>
      <View style={styles.haut}>
        <View style={{ flex: 1 }}>
          <Text style={typo.petit}>Ma boutique</Text>
          <Text style={typo.titre} numberOfLines={1}>{boutique.nomVendeur}</Text>
        </View>
        <BoutonIcone icone="settings-outline" onPress={onParametres} libelle="Paramètres" />
      </View>

      <LinearGradient colors={[couleurs.marque, couleurs.marqueFonce]} start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }}
                      style={[styles.bandeau, ombre.forte]}>
        <View style={styles.stats}>
          <View style={{ flex: 1 }}>
            <Text style={styles.statNombre}>{enVente}</Text>
            <Text style={styles.statLabel}>{enVente > 1 ? 'articles en vente' : 'article en vente'}</Text>
          </View>
          <View style={styles.separateur} />
          <View style={{ flex: 1 }}>
            <Text style={styles.statNombre}>{produits.length}</Text>
            <Text style={styles.statLabel}>au total</Text>
          </View>
        </View>
        <Pressable onPress={partager} style={({ pressed }) => [styles.partager, pressed && { opacity: 0.85 }]}
                   accessibilityRole="button">
          <Ionicons name="share-social" size={18} color={couleurs.marque} />
          <Text style={styles.partagerTexte}>Partager ma boutique</Text>
        </Pressable>
      </LinearGradient>

      {produits.length > 0 && <Text style={typo.h2}>Mes articles</Text>}
    </View>
  );

  return (
    <View style={{ flex: 1, backgroundColor: couleurs.fond }}>
      <FlatList
        data={produits}
        keyExtractor={(p) => p.id}
        numColumns={2}
        columnWrapperStyle={{ gap: espace.m }}
        contentContainerStyle={{ paddingTop: marges.top + espace.m, paddingHorizontal: espace.xl,
                                 paddingBottom: marges.bottom + 110, gap: espace.m }}
        ListHeaderComponent={entete}
        refreshControl={<RefreshControl refreshing={chargement} onRefresh={charger} tintColor={couleurs.marque} />}
        ListEmptyComponent={!chargement ? (
          <View style={styles.vide}>
            <Pictogramme icone="pricetags-outline" taille={72} />
            <Text style={[typo.h2, { textAlign: 'center' }]}>Aucun article pour l&apos;instant</Text>
            <Text style={[typo.corps, { textAlign: 'center' }]}>
              Prenez une photo, décrivez l&apos;article à voix haute : votre fiche est prête en quelques secondes.
            </Text>
          </View>
        ) : null}
        renderItem={({ item }) => (
          <View style={[styles.carte, { width: largeurCarte }, !item.actif && { opacity: 0.6 }]}>
            <Image source={{ uri: item.imageUrl }} style={styles.image} />
            <Pressable onPress={() => basculer(item)} hitSlop={6} accessibilityRole="button"
                       accessibilityLabel={item.actif ? 'Masquer l\'article' : 'Remettre en vente'}
                       style={styles.visibilite}>
              <Ionicons name={item.actif ? 'eye-outline' : 'eye-off-outline'} size={18} color={couleurs.texte} />
            </Pressable>
            <View style={styles.infos}>
              <Text style={styles.nom} numberOfLines={2}>{item.nomProduit}</Text>
              <Text style={styles.prix}>{formaterPrix(item.prix)}</Text>
              <View style={{ flexDirection: 'row', gap: 6, flexWrap: 'wrap' }}>
                <Pastille texte={item.actif ? 'En vente' : 'Masqué'} active={item.actif} />
                {item.taille && (
                  <View style={styles.taille}><Text style={styles.tailleTexte}>{item.taille}</Text></View>
                )}
              </View>
            </View>
          </View>
        )}
      />

      <Pressable onPress={() => { vibrer(); onAjouter(); }} accessibilityRole="button" testID="bouton-ajouter"
                 style={({ pressed }) => [styles.fab, { bottom: marges.bottom + espace.xl },
                   pressed && { transform: [{ scale: 0.96 }] }]}>
        <LinearGradient colors={[couleurs.marque, couleurs.marqueFonce]} style={[styles.fabInterieur, ombre.forte]}>
          <Ionicons name="add" size={26} color={couleurs.blanc} />
          <Text style={styles.fabTexte}>Nouvel article</Text>
        </LinearGradient>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  haut: { flexDirection: 'row', alignItems: 'center', gap: espace.m },
  bandeau: { borderRadius: rayon.l, padding: espace.xl, gap: espace.xl },
  stats: { flexDirection: 'row', alignItems: 'center' },
  statNombre: { fontSize: 32, fontWeight: '800', color: couleurs.blanc, letterSpacing: -1 },
  statLabel: { fontSize: 14, color: 'rgba(255,255,255,0.8)' },
  separateur: { width: 1, alignSelf: 'stretch', backgroundColor: 'rgba(255,255,255,0.25)', marginHorizontal: espace.l },
  partager: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: espace.s,
    backgroundColor: couleurs.blanc,
    borderRadius: rayon.m,
    paddingVertical: espace.m,
  },
  partagerTexte: { fontSize: 15, fontWeight: '700', color: couleurs.marque },
  carte: {
    backgroundColor: couleurs.surface,
    borderRadius: rayon.l,
    overflow: 'hidden',
    ...ombre.douce,
  },
  image: { width: '100%', aspectRatio: 1, backgroundColor: couleurs.bord },
  visibilite: {
    position: 'absolute',
    top: espace.s,
    right: espace.s,
    width: 34,
    height: 34,
    borderRadius: 17,
    backgroundColor: 'rgba(255,255,255,0.92)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  infos: { padding: espace.m, gap: 6 },
  nom: { fontSize: 15, lineHeight: 20, fontWeight: '600', color: couleurs.texte },
  prix: { fontSize: 16, fontWeight: '800', color: couleurs.marque },
  taille: { paddingHorizontal: 8, paddingVertical: 4, borderRadius: rayon.rond, backgroundColor: couleurs.fond },
  tailleTexte: { fontSize: 12, fontWeight: '600', color: couleurs.texte2 },
  vide: { alignItems: 'center', gap: espace.m, paddingVertical: espace.xxl, paddingHorizontal: espace.l },
  fab: { position: 'absolute', right: espace.xl, left: espace.xl, alignItems: 'center' },
  fabInterieur: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: espace.s,
    paddingHorizontal: espace.xl,
    height: 58,
    borderRadius: rayon.rond,
  },
  fabTexte: { fontSize: 16, fontWeight: '700', color: couleurs.blanc },
});
