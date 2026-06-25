import React from 'react';
import {
  StyleSheet,
  Text,
  View,
  Dimensions,
  TouchableOpacity,
  ScrollView,
} from 'react-native';

// --- CUSTOM SVG ICON FALLBACKS (Pure React Native Vector path representations) ---
const StepsIcon = ({ size = 20, color = '#ADFF2F' }) => (
  <View style={[styles.iconContainer, { width: size * 1.8, height: size * 1.8 }]}>
    <Text style={{ fontSize: size }}>👟</Text>
  </View>
);

const CaloriesIcon = ({ size = 20, color = '#FF4500' }) => (
  <View style={[styles.iconContainer, { width: size * 1.8, height: size * 1.8 }]}>
    <Text style={{ fontSize: size }}>🔥</Text>
  </View>
);

const WaterIcon = ({ size = 20, color = '#00BFFF' }) => (
  <View style={[styles.iconContainer, { width: size * 1.8, height: size * 1.8 }]}>
    <Text style={{ fontSize: size }}>💧</Text>
  </View>
);

// --- TS INTERFACES ---
export interface ActivitySummaryProps {
  currentSteps?: number;
  stepGoal?: number;
  currentCalories?: number;
  calorieGoal?: number;
  currentWaterMl?: number;
  waterGoalMl?: number;
  onRefresh?: () => void;
  onCardPress?: (metric: 'steps' | 'calories' | 'water') => void;
}

export const ProfileActivitySummary: React.FC<ActivitySummaryProps> = ({
  currentSteps = 8420,
  stepGoal = 10000,
  currentCalories = 480,
  calorieGoal = 700,
  currentWaterMl = 1250,
  waterGoalMl = 2500,
  onRefresh,
  onCardPress,
}) => {
  // Utility percentage math
  const stepsPercentage = Math.min((currentSteps / stepGoal) * 100, 100);
  const caloriesPercentage = Math.min((currentCalories / calorieGoal) * 100, 100);
  const waterPercentage = Math.min((currentWaterMl / waterGoalMl) * 100, 100);

  return (
    <View style={styles.container}>
      {/* Title & Headline section */}
      <View style={styles.headerRow}>
        <View>
          <Text style={styles.sectionTitle}>⚡ Today's Vital Metrics</Text>
          <Text style={styles.subTitle}>Daily metabolic log & training thresholds</Text>
        </View>
        {onRefresh && (
          <TouchableOpacity style={styles.refreshButton} onPress={onRefresh}>
            <Text style={styles.refreshIcon}>🔄</Text>
          </TouchableOpacity>
        )}
      </View>

      {/* Horizontal Cards Grid for Compact & Medium Mobile Views */}
      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={styles.cardsScroll}
      >
        {/* Card 1: DAILY STEPS */}
        <TouchableOpacity
          activeOpacity={0.85}
          style={[styles.activityCard, styles.stepsCardBorder]}
          onPress={() => onCardPress && onCardPress('steps')}
        >
          <View style={styles.cardHeader}>
            <StepsIcon size={24} />
            <Text style={[styles.badgeText, { color: '#ADFF2F', backgroundColor: 'rgba(173, 255, 47, 0.12)' }]}>
              {stepsPercentage.toFixed(0)}%
            </Text>
          </View>
          
          <View style={styles.valueSection}>
            <Text style={styles.cardLabel}>Steps Walked</Text>
            <Text style={styles.vitalCount}>
              {currentSteps.toLocaleString()}
              <Text style={styles.goalUnit}> / {stepGoal.toLocaleString()} step</Text>
            </Text>
          </View>

          {/* Micro Progress Track */}
          <View style={styles.progressBarBg}>
            <View style={[styles.progressBarFill, { width: `${stepsPercentage}%`, backgroundColor: '#ADFF2F' }]} />
          </View>
        </TouchableOpacity>

        {/* Card 2: CALORIES BURNED */}
        <TouchableOpacity
          activeOpacity={0.85}
          style={[styles.activityCard, styles.caloriesCardBorder]}
          onPress={() => onCardPress && onCardPress('calories')}
        >
          <View style={styles.cardHeader}>
            <CaloriesIcon size={24} />
            <Text style={[styles.badgeText, { color: '#FF7F50', backgroundColor: 'rgba(255, 127, 80, 0.12)' }]}>
              {caloriesPercentage.toFixed(0)}%
            </Text>
          </View>
          
          <View style={styles.valueSection}>
            <Text style={styles.cardLabel}>Caloric Burn</Text>
            <Text style={styles.vitalCount}>
              {currentCalories}
              <Text style={styles.goalUnit}> / {calorieGoal} kcal</Text>
            </Text>
          </View>

          {/* Micro Progress Track */}
          <View style={styles.progressBarBg}>
            <View style={[styles.progressBarFill, { width: `${caloriesPercentage}%`, backgroundColor: '#FF7F50' }]} />
          </View>
        </TouchableOpacity>

        {/* Card 3: WATER INTAKE */}
        <TouchableOpacity
          activeOpacity={0.85}
          style={[styles.activityCard, styles.waterCardBorder]}
          onPress={() => onCardPress && onCardPress('water')}
        >
          <View style={styles.cardHeader}>
            <WaterIcon size={24} />
            <Text style={[styles.badgeText, { color: '#00BFFF', backgroundColor: 'rgba(0, 191, 255, 0.12)' }]}>
              {waterPercentage.toFixed(0)}%
            </Text>
          </View>
          
          <View style={styles.valueSection}>
            <Text style={styles.cardLabel}>Hydration Volume</Text>
            <Text style={styles.vitalCount}>
              {currentWaterMl}
              <Text style={styles.goalUnit}> / {waterGoalMl} ml</Text>
            </Text>
          </View>

          {/* Micro Progress Track */}
          <View style={styles.progressBarBg}>
            <View style={[styles.progressBarFill, { width: `${waterPercentage}%`, backgroundColor: '#00BFFF' }]} />
          </View>
        </TouchableOpacity>
      </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    marginVertical: 16,
    paddingHorizontal: 16,
    width: '100%',
  },
  headerRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 14,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: '900',
    color: '#F8F9FA',
    letterSpacing: 0.5,
  },
  subTitle: {
    fontSize: 11,
    color: '#8A9A86',
    marginTop: 2,
  },
  refreshButton: {
    padding: 8,
    borderRadius: 8,
    backgroundColor: '#2A2A2A',
  },
  refreshIcon: {
    fontSize: 14,
  },
  cardsScroll: {
    paddingRight: 16,
    gap: 12,
  },
  activityCard: {
    width: Dimensions.get('window').width * 0.65,
    backgroundColor: '#1E2220',
    borderRadius: 16,
    padding: 16,
    borderWidth: 1,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 5,
    elevation: 4,
    justifyContent: 'space-between',
  },
  stepsCardBorder: {
    borderColor: 'rgba(173, 255, 47, 0.18)',
  },
  caloriesCardBorder: {
    borderColor: 'rgba(255, 127, 80, 0.18)',
  },
  waterCardBorder: {
    borderColor: 'rgba(0, 191, 255, 0.18)',
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 4,
  },
  iconContainer: {
    borderRadius: 12,
    backgroundColor: 'rgba(255,255,255,0.05)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  badgeText: {
    fontSize: 10,
    fontWeight: '900',
    paddingVertical: 3,
    paddingHorizontal: 8,
    borderRadius: 8,
    overflow: 'hidden',
  },
  valueSection: {
    marginTop: 12,
    marginBottom: 12,
  },
  cardLabel: {
    fontSize: 12,
    color: '#ADB5BD',
    fontWeight: '700',
  },
  vitalCount: {
    fontSize: 22,
    color: '#FFFFFF',
    fontWeight: '900',
    marginTop: 4,
  },
  goalUnit: {
    fontSize: 11,
    color: '#6C757D',
    fontWeight: '500',
  },
  progressBarBg: {
    height: 4,
    backgroundColor: 'rgba(255, 255, 255, 0.1)',
    borderRadius: 2,
    width: '100%',
    overflow: 'hidden',
  },
  progressBarFill: {
    height: '100%',
    borderRadius: 2,
  },
});

export default ProfileActivitySummary;
