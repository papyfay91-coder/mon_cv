import { useCallback, useEffect, useState } from 'react';
import { FlatList, Image, Pressable, RefreshControl, Share, Switch, Text, View } from 'react-native';
import { api, Boutique, formaterPrix, Produit, VITRINE_URL } from '../api';
import { couleurs, styles } from '../theme';

export default function MesProduits({ boutique, onAjouter, onParametres }: {
  boutique: Boutique;
  onAjouter: () => void;
  onParametres: () => void;
}) {
  const [produits, setProduits] = useState<Produit[]>([]);
  const [chargement, setChargement] = useState(false);

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
    const maj = await api.basculerProduit(p.id, !p.actif);
    setProduits((liste) => liste.map((x) => (x.id === maj.id ? maj : x)));
  };

  const partager = () =>
    Share.share({ message: `Découvrez ma boutique ${boutique.nomVendeur} : ${VITRINE_URL}/boutique/${boutique.id}` });

  return (
    <View style={[styles.ecran, { paddingBottom: 0 }]}>
      <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}>
        <Text style={styles.titre} numberOfLines={1}>{boutique.nomVendeur}</Text>
        <Pressable onPress={onParametres} hitSlop={16} accessibilityLabel="Paramètres">
          <Text style={{ fontSize: 28 }}>⚙️</Text>
        </Pressable>
      </View>

      <View style={{ flexDirection: 'row', gap: 10 }}>
        <Pressable style={[styles.bouton, { flex: 2 }]} onPress={onAjouter}>
          <Text style={styles.boutonTexte}>📸 + 🎙️ Ajouter</Text>
        </Pressable>
        <Pressable style={[styles.boutonSecondaire, { flex: 1, justifyContent: 'center' }]} onPress={partager}>
          <Text style={styles.boutonSecondaireTexte}>🔗 Partager</Text>
        </Pressable>
      </View>

      <FlatList
        data={produits}
        keyExtractor={(p) => p.id}
        refreshControl={<RefreshControl refreshing={chargement} onRefresh={charger} />}
        ListEmptyComponent={!chargement ? <Text style={styles.doux}>Aucun article. Appuyez sur « Ajouter ».</Text> : null}
        contentContainerStyle={{ gap: 10, paddingBottom: 24 }}
        renderItem={({ item }) => (
          <View style={{ flexDirection: 'row', gap: 12, alignItems: 'center', backgroundColor: couleurs.carte,
                         borderRadius: 14, padding: 10, opacity: item.actif ? 1 : 0.5 }}>
            <Image source={{ uri: item.imageUrl }} style={{ width: 72, height: 72, borderRadius: 10 }} />
            <View style={{ flex: 1 }}>
              <Text style={styles.texte} numberOfLines={2}>{item.nomProduit}</Text>
              <Text style={{ color: couleurs.accent, fontWeight: '700', fontSize: 16 }}>{formaterPrix(item.prix)}</Text>
              {item.taille && <Text style={styles.doux}>Taille {item.taille}</Text>}
            </View>
            <Switch value={item.actif} onValueChange={() => basculer(item)} accessibilityLabel="En vente" />
          </View>
        )}
      />
    </View>
  );
}
