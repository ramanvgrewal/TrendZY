import SkeletonCard from './SkeletonCard';

export default function SkeletonRow({ count = 5 }) {
  return (
    <div className="w-full overflow-hidden mb-12">
      <div className="flex gap-6 overflow-x-hidden p-1 pb-4">
        {[...Array(count)].map((_, i) => (
          <SkeletonCard key={i} />
        ))}
      </div>
    </div>
  );
}
